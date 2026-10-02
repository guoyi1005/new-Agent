package com.example.appbackend.service.impl;

import com.example.appbackend.service.LocalJobCrawler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 成都人才网（job.rc114.com）职位列表。
 *
 * <p>列表页是服务端直出 HTML，每页 20 条，结构为
 * {@code <div class="joblist">} 里依次是岗位名 / 单位 / 学历 / 工作地点 / 月薪 / 刷新日期。
 * 工作地点就是行政区（锦江区、高新区等），是本页「成都本地就业」区域统计的主要来源。</p>
 */
@Component
public class Rc114JobCrawler implements LocalJobCrawler {

    private static final String SOURCE_KEY = "rc114";
    private static final String SOURCE_NAME = "成都人才网";
    private static final String LIST_URL = "https://job.rc114.com/JobSearchCate.aspx";
    private static final String BLOCK_MARK = "<div class=\"joblist\">";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/M/d");

    private static final Pattern JOB_ID = Pattern.compile("JobInfo\\.aspx\\?jobid=([0-9A-Za-z\\-]+)");
    private static final Pattern TITLE = Pattern.compile("(?s)class=\"jobname\">\\s*<a[^>]*>\\s*([^<]+)");
    private static final Pattern COMPANY = Pattern.compile("(?s)class=\"unitname\">\\s*<a[^>]*>\\s*([^<]+)");
    private static final Pattern EDUCATION = Pattern.compile("(?s)class=\"edu\">\\s*([^<]*)");
    private static final Pattern DISTRICT = Pattern.compile("(?s)class=\"workcity\">\\s*([^<]*)");
    private static final Pattern SALARY_AND_DATE =
            Pattern.compile("(?s)class=\"salary\">\\s*([^<]*)</li>\\s*<li>\\s*([^<]*)");

    private final LocalJobFetcher fetcher;
    private final boolean enabled;
    private final int pages;
    private final long pauseMillis;
    private final List<String> directionKeywords;

    public Rc114JobCrawler(
            LocalJobFetcher fetcher,
            @Value("${local-job.crawl.rc114.enabled:${local-job.crawl.enabled:true}}") boolean enabled,
            @Value("${local-job.crawl.rc114.pages:3}") int pages,
            @Value("${local-job.crawl.pause-millis:400}") long pauseMillis,
            @Value("${local-job.crawl.direction-keywords:Python,Java,Vue,Data}") String directionKeywords) {
        this.fetcher = fetcher;
        this.enabled = enabled;
        this.pages = Math.max(1, pages);
        this.pauseMillis = Math.max(0, pauseMillis);
        this.directionKeywords = new ArrayList<>();
        for (String item : directionKeywords.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                this.directionKeywords.add(trimmed);
            }
        }
    }

    @Override
    public String sourceKey() {
        return SOURCE_KEY;
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public List<CrawledJob> crawl() {
        List<CrawledJob> jobs = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            String url = page == 1 ? LIST_URL : LIST_URL + "?page=" + page;
            String html = fetcher.get(url, LIST_URL);
            if (!StringUtils.hasText(html)) {
                break;
            }
            List<CrawledJob> pageJobs = parse(html);
            jobs.addAll(pageJobs);
            if (pageJobs.isEmpty()) {
                break;
            }
            pause();
        }
        // 各站最新一页以非技术岗为主，按方向关键词再补一轮，让推荐区能匹配到学生的目标方向
        for (String direction : directionKeywords) {
            String html = fetcher.get(LIST_URL + "?KeyWord=" + URLEncoder.encode(direction, StandardCharsets.UTF_8), LIST_URL);
            jobs.addAll(parse(html));
            pause();
        }
        return jobs;
    }

    static List<CrawledJob> parse(String html) {
        List<CrawledJob> jobs = new ArrayList<>();
        int cursor = 0;
        while (true) {
            int start = html.indexOf(BLOCK_MARK, cursor);
            if (start < 0) {
                break;
            }
            int end = html.indexOf("</ul>", start);
            if (end < 0) {
                break;
            }
            String block = html.substring(start, end);
            cursor = end;

            String title = clean(first(block, TITLE));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            String jobId = first(block, JOB_ID);
            String salary = "";
            String dateText = "";
            Matcher salaryMatcher = SALARY_AND_DATE.matcher(block);
            if (salaryMatcher.find()) {
                salary = clean(salaryMatcher.group(1));
                dateText = clean(salaryMatcher.group(2));
            }
            jobs.add(new CrawledJob(
                    jobId,
                    title,
                    clean(first(block, COMPANY)),
                    "成都",
                    clean(first(block, DISTRICT)),
                    salary,
                    clean(first(block, EDUCATION)),
                    jobId.isBlank() ? "" : "https://job.rc114.com/JobInfo.aspx?jobid=" + jobId,
                    parseDate(dateText),
                    ""
            ));
        }
        return jobs;
    }

    private static LocalDateTime parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT).atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String first(String source, Pattern pattern) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private void pause() {
        if (pauseMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(pauseMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
