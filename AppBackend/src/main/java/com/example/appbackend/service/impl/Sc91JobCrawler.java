package com.example.appbackend.service.impl;

import com.example.appbackend.service.LocalJobCrawler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 公共招聘网（四川 / 成都）职位搜索接口。
 *
 * <p>站点列表由 ajax 返回 JSON：{@code datalist[]} 里是岗位名 acb213、单位 aab004、
 * 地区 acb217、学历 aac011_dsc、月薪 acb21l、发布时间 aae044。成都站与四川站是同一套数据，
 * 入库时按岗位去重，同一岗位只保留一条并记录出现在哪些平台。</p>
 */
public class Sc91JobCrawler implements LocalJobCrawler {

    private static final String SEARCH_PATH = "/app/search/jobSearch.shtml";
    private static final String SEARCH_API = "/app/search/jobSearchAction!searchJob.do";
    /** 成都行政区划代码，接口按它筛选本地岗位。 */
    private static final String CHENGDU_AREA_CODE = "510100000";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalJobFetcher fetcher;
    private final ObjectMapper objectMapper;
    private final String sourceKey;
    private final String sourceName;
    private final String baseUrl;
    private final boolean enabled;
    private final boolean chengduOnly;
    private final int pages;
    private final long pauseMillis;
    private final List<String> directionKeywords;

    public Sc91JobCrawler(
            LocalJobFetcher fetcher,
            ObjectMapper objectMapper,
            String sourceKey,
            String sourceName,
            String baseUrl,
            boolean enabled,
            boolean chengduOnly,
            int pages,
            long pauseMillis,
            String directionKeywords) {
        this.fetcher = fetcher;
        this.objectMapper = objectMapper;
        this.sourceKey = sourceKey;
        this.sourceName = sourceName;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.enabled = enabled;
        this.chengduOnly = chengduOnly;
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
        return sourceKey;
    }

    @Override
    public String sourceName() {
        return sourceName;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public List<CrawledJob> crawl() {
        List<CrawledJob> jobs = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            String body = fetcher.postForm(baseUrl + SEARCH_API, form(page, ""), baseUrl + SEARCH_PATH);
            List<CrawledJob> pageJobs = parse(body, chengduOnly);
            jobs.addAll(pageJobs);
            if (pageJobs.isEmpty()) {
                break;
            }
            pause();
        }
        // 按方向关键词再补一轮，保证库里也有学生目标方向的岗位
        for (String direction : directionKeywords) {
            String body = fetcher.postForm(baseUrl + SEARCH_API, form(1, direction), baseUrl + SEARCH_PATH);
            jobs.addAll(parse(body, chengduOnly));
            pause();
        }
        return jobs;
    }

    private MultiValueMap<String, String> form(int page, String keyword) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("keyword", keyword);
        form.add("page", String.valueOf(page));
        form.add("city", CHENGDU_AREA_CODE);
        form.add("zwlb", "1");
        form.add("dwlx", "1");
        form.add("gzxz", "0");
        form.add("gzjy", "0");
        form.add("xl", "0");
        form.add("xz", "1");
        form.add("acc210", "");
        return form;
    }

    List<CrawledJob> parse(String body, boolean chengduOnly) {
        List<CrawledJob> jobs = new ArrayList<>();
        if (!StringUtils.hasText(body)) {
            return jobs;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode list = root.path("datalist");
            if (!list.isArray()) {
                return jobs;
            }
            for (JsonNode item : list) {
                String title = text(item, "acb213");
                if (!StringUtils.hasText(title)) {
                    continue;
                }
                String location = text(item, "acb217");
                if (chengduOnly && !location.contains("成都")) {
                    continue;
                }
                jobs.add(new CrawledJob(
                        text(item, "acb210"),
                        title,
                        text(item, "aab004"),
                        "成都",
                        districtOf(location),
                        salaryOf(item),
                        text(item, "aac011_dsc"),
                        "",
                        parseDate(text(item, "aae044")),
                        ""
                ));
            }
        } catch (Exception ignored) {
            // 返回的不是预期 JSON 时按空结果处理，由上层记录该来源本次没有数据
        }
        return jobs;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private static String salaryOf(JsonNode item) {
        int value = item.path("acb21l").asInt(0);
        return value > 0 ? value + " 元/月" : "";
    }

    /** 接口只给到「四川省成都市」这一级，能取到区县时再填区县。 */
    private static String districtOf(String location) {
        if (!StringUtils.hasText(location)) {
            return "";
        }
        String value = location.replace("四川省", "").replace("成都市", "").trim();
        return value.length() <= 6 ? value : "";
    }

    private static LocalDateTime parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            String normalized = value.trim();
            if (normalized.length() == 10) {
                return LocalDate.parse(normalized).atStartOfDay();
            }
            return LocalDateTime.parse(normalized, DATE_TIME_FORMAT);
        } catch (Exception ignored) {
            return null;
        }
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
