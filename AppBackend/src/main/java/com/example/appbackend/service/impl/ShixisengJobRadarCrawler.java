package com.example.appbackend.service.impl;

import com.example.appbackend.dto.JobImportRequest;
import com.example.appbackend.service.JobFingerprint;
import com.example.appbackend.service.JobTitleNormalizer;
import com.example.appbackend.service.SkillExtractor;
import com.example.appbackend.service.impl.MarketJobRadarFetcher.RadarStoppedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 岗位雷达：抓取实习僧（s.shixiseng.com / www.shixiseng.com）公开的实习岗位。
 *
 * <p>只使用站点公开的搜索列表页与职位详情页，HTTP 直接抓取，不登录、不带 Cookie、
 * 不使用站内接口，也不研究站点的字体混淆（列表卡片里的数字被字体替换，详情页是正常文本，
 * 因此所有字段都以详情页为准）。请求之间有 1.5-3 秒随机间隔，单轮有请求上限；
 * 一旦遇到 403/429 或验证页就停止整轮抓取，不做任何绕过。</p>
 *
 * <p>抓取结果转换为 {@link JobImportRequest}，交给已有的岗位导入服务写库，
 * 复用同一套质量门槛、去重与热门快照计算，不另建一套数据。</p>
 */
@Component
public class ShixisengJobRadarCrawler {

    private static final Logger log = LoggerFactory.getLogger(ShixisengJobRadarCrawler.class);

    private static final String SOURCE = "shixiseng";
    private static final String LIST_URL = "https://s.shixiseng.com/interns";
    private static final String ALLOWED_HOST = "shixiseng.com";
    private static final String JOB_TYPE = "internship";

    /** 详情页正文里说明岗位已下线的字样。 */
    private static final String OFFLINE_MARK = "当前职位已下线";
    private static final Pattern PRIVATE_USE = Pattern.compile("[\\ue000-\\uf8ff]");
    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
    private static final Pattern TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern ENTITY = Pattern.compile("&#?[0-9a-zA-Z]+;");

    private static final Pattern LIST_LINK =
            Pattern.compile("href=\"(https?://[^\"]*?/intern/(inn_[a-z0-9]+)[^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_TITLE = Pattern.compile("(?is)<title>(.*?)</title>");
    private static final Pattern JOB_NAME = Pattern.compile("(?is)class=\"new_job_name\"[^>]*>(.*?)</div>");
    private static final Pattern JOB_DATE = Pattern.compile("(?is)class=\"job_date\"[^>]*>(.*?)</div>");
    private static final Pattern JOB_MONEY = Pattern.compile("(?is)class=\"job_money[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern JOB_POSITION = Pattern.compile("(?is)class=\"job_position\"[^>]*>(.*?)</span>");
    private static final Pattern JOB_ACADEMIC = Pattern.compile("(?is)class=\"job_academic\"[^>]*>(.*?)</span>");
    private static final Pattern JOB_WEEK = Pattern.compile("(?is)class=\"job_week[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern JOB_TIME = Pattern.compile("(?is)class=\"job_time[^\"]*\"[^>]*>(.*?)</span>");
    private static final Pattern COM_POSITION = Pattern.compile("(?is)class=\"com_position\"[^>]*>(.*?)</span>");
    private static final Pattern JOB_DETAIL = Pattern.compile("(?is)class=\"job_detail\"[^>]*>(.*?)</div>");
    private static final Pattern DATE_TIME = Pattern.compile("(20\\d{2}[-/]\\d{1,2}[-/]\\d{1,2}(?:\\s+\\d{1,2}:\\d{2}(?::\\d{2})?)?)");
    private static final Pattern DEADLINE = Pattern.compile("截止日期\\s*[:：]?\\s*(20\\d{2}[-/]\\d{1,2}[-/]\\d{1,2})");
    private static final Pattern SALARY_RANGE = Pattern.compile(
            "(?i)(\\d+(?:\\.\\d+)?)\\s*[-~至到]\\s*(\\d+(?:\\.\\d+)?)\\s*(k|千|元\\s*/\\s*天|元\\s*/\\s*日|/天|万(?:\\s*/\\s*年|年)?)?(?:[^0-9]{0,8}(\\d+)\\s*薪)?");
    private static final Pattern LOCATION_SPLIT = Pattern.compile("[/·,，\\s]+");

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-M-d");
    private static final List<String> REQUIREMENT_MARKS =
            List.of("任职要求", "岗位要求", "任职资格", "招聘要求", "职位要求");

    private final MarketJobRadarFetcher fetcher;
    private final JobTitleNormalizer titleNormalizer;
    private final SkillExtractor skillExtractor;
    private final JobFingerprint fingerprint;

    private final long minDelayMillis;
    private final long maxDelayMillis;
    private final int maxAgeDays;
    private final int maxDetailRequests;
    private final boolean requireDirectionMatch;

    public ShixisengJobRadarCrawler(
            MarketJobRadarFetcher fetcher,
            JobTitleNormalizer titleNormalizer,
            SkillExtractor skillExtractor,
            JobFingerprint fingerprint,
            @Value("${market-job.radar.min-delay-millis:1500}") long minDelayMillis,
            @Value("${market-job.radar.max-delay-millis:3000}") long maxDelayMillis,
            @Value("${market-job.radar.max-age-days:90}") int maxAgeDays,
            @Value("${market-job.radar.max-detail-requests:120}") int maxDetailRequests,
            @Value("${market-job.radar.require-direction-match:true}") boolean requireDirectionMatch) {
        this.fetcher = fetcher;
        this.titleNormalizer = titleNormalizer;
        this.skillExtractor = skillExtractor;
        this.fingerprint = fingerprint;
        this.minDelayMillis = Math.max(1000, minDelayMillis);
        this.maxDelayMillis = Math.max(this.minDelayMillis, maxDelayMillis);
        this.maxAgeDays = Math.max(7, maxAgeDays);
        this.maxDetailRequests = Math.max(1, maxDetailRequests);
        this.requireDirectionMatch = requireDirectionMatch;
    }

    /**
     * 抓取一批实习岗位。
     *
     * @param keywords        搜索关键词，逐个人工指定的方向
     * @param perKeywordLimit 每个关键词最多取多少条详情
     * @return 已通过质量门槛、可直接交给导入服务的记录
     */
    public List<JobImportRequest> crawl(List<String> keywords, int perKeywordLimit) {
        List<JobImportRequest> result = new ArrayList<>();
        Set<String> seenJobIds = new LinkedHashSet<>();
        Map<String, Integer> skipReasons = new LinkedHashMap<>();
        int detailBudget = maxDetailRequests;
        int index = 0;

        for (String keyword : keywords) {
            if (detailBudget <= 0) {
                log.info("岗位雷达已达单轮请求上限 {}，提前结束", maxDetailRequests);
                break;
            }
            index += 1;
            String listUrl = LIST_URL + "?from=menu&keyword="
                    + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            pause(index == 1);
            String listHtml = fetcher.get(listUrl);
            Map<String, String> discovered = parseList(listHtml);
            log.info("岗位雷达关键词 [{}] 发现 {} 条候选", keyword, discovered.size());

            int taken = 0;
            for (Map.Entry<String, String> entry : discovered.entrySet()) {
                if (taken >= Math.max(1, perKeywordLimit) || detailBudget <= 0) {
                    break;
                }
                if (!seenJobIds.add(entry.getKey())) {
                    continue;
                }
                taken += 1;
                detailBudget -= 1;
                pause(false);
                String detailHtml = fetcher.get(entry.getValue());
                Parsed parsed = parseDetail(detailHtml, entry.getKey(), entry.getValue());
                if (parsed.request() != null) {
                    // 站点搜索是模糊匹配，会带出餐饮、行政、奢侈品电商等无关实习；
                    // 自动入库没有人逐条复核，这里按配置的方向关键词再过一道，
                    // 岗位里至少命中一个方向词、或本身命中两项以上技术栈才算相关。
                    if (requireDirectionMatch && !matchesDirection(parsed.request(), keywords)) {
                        skipReasons.merge("方向不匹配", 1, Integer::sum);
                    } else {
                        result.add(parsed.request());
                    }
                } else {
                    skipReasons.merge(parsed.skipReason(), 1, Integer::sum);
                }
            }
        }
        if (!skipReasons.isEmpty()) {
            log.info("岗位雷达跳过 {} 条：{}", skipReasons.values().stream().mapToInt(Integer::intValue).sum(), skipReasons);
        }
        return result;
    }

    private boolean matchesDirection(JobImportRequest request, List<String> directionKeywords) {
        String text = String.join(" ",
                request.jobTitle() == null ? "" : request.jobTitle(),
                request.description() == null ? "" : request.description(),
                request.requirements() == null ? "" : request.requirements()).toLowerCase(Locale.ROOT);
        for (String keyword : directionKeywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return request.skills() != null && request.skills().size() >= 2;
    }

    /** 列表页只用来发现详情链接；岗位字段一律以详情页为准。 */
    Map<String, String> parseList(String html) {
        Map<String, String> discovered = new LinkedHashMap<>();
        Matcher matcher = LIST_LINK.matcher(html == null ? "" : html);
        while (matcher.find()) {
            String url = matcher.group(1);
            String jobId = matcher.group(2);
            if (!isAllowedUrl(url) || discovered.containsKey(jobId)) {
                continue;
            }
            discovered.put(jobId, url.split("\\?", 2)[0]);
        }
        return discovered;
    }

    /** 解析职位详情页；字段不完整或明显过期时不写入，并给出跳过原因。 */
    Parsed parseDetail(String html, String jobId, String url) {
        if (html == null || html.isBlank() || !isAllowedUrl(url)) {
            return Parsed.skipped("页面不可解析");
        }
        String pageText = plainText(html);
        boolean offline = pageText.contains(OFFLINE_MARK);

        String title = plainText(first(JOB_NAME, html));
        String pageTitle = plainText(first(PAGE_TITLE, html));
        if (!StringUtils.hasText(title)) {
            title = pageTitle.replaceAll("\\s*[-|｜].*$", "").trim();
        }
        title = clip(title, 240);
        if (!StringUtils.hasText(title)) {
            return Parsed.skipped("缺少岗位名称");
        }

        String company = clip(companyFromPageTitle(pageTitle), 240);
        if (!StringUtils.hasText(company)) {
            company = clip(companyFromLocation(plainText(first(COM_POSITION, html))), 240);
        }
        if (!StringUtils.hasText(company)) {
            return Parsed.skipped("缺少公司名称");
        }

        String salaryText = clip(plainText(first(JOB_MONEY, html)), 120);
        String locationText = plainText(first(JOB_POSITION, html));
        String addressText = plainText(first(COM_POSITION, html));
        String city = clip(resolveCity(locationText, addressText), 100);
        String district = clip(resolveDistrict(addressText, city), 100);
        String education = clip(plainText(first(JOB_ACADEMIC, html)), 60);

        List<String> experienceParts = new ArrayList<>();
        experienceParts.addAll(all(JOB_WEEK, html));
        experienceParts.addAll(all(JOB_TIME, html));
        String experience = clip(String.join(" · ", experienceParts.stream()
                .map(this::plainText)
                .filter(StringUtils::hasText)
                .toList()), 80);

        String detailText = plainText(first(JOB_DETAIL, html));
        String description = clip(detailText, 20000);
        String requirements = description;
        int markIndex = firstIndexOf(detailText);
        if (markIndex > 0) {
            description = clip(detailText.substring(0, markIndex), 20000);
            requirements = clip(detailText.substring(markIndex), 20000);
        }
        if (!StringUtils.hasText(description) || description.length() < 30) {
            description = null;
        }

        LocalDateTime publishTime = resolvePublishTime(plainText(first(JOB_DATE, html)), pageText);
        boolean recent = publishTime == null
                || !publishTime.toLocalDate().isBefore(LocalDate.now().minusDays(maxAgeDays));
        LocalDate deadline = resolveDeadline(pageText);
        boolean active = !offline && (deadline == null || !deadline.isBefore(LocalDate.now()));
        if (offline) {
            return Parsed.skipped("岗位已下线");
        }
        if (!active) {
            return Parsed.skipped("已过截止日期");
        }
        if (!recent) {
            return Parsed.skipped("刷新时间超过 " + maxAgeDays + " 天");
        }

        String normalizedTitle = titleNormalizer.normalize(title);
        List<String> skills = skillExtractor.extract(
                        (description == null ? "" : description) + " " + (requirements == null ? "" : requirements))
                .stream().map(SkillExtractor.Skill::name).toList();

        ParsedSalary salary = parseSalary(salaryText);
        String fingerprintValue = fingerprint.create(company, normalizedTitle, city, salaryText);
        int quality = qualityScore(title, company, city, salaryText, description, requirements, education, experience, publishTime, url);
        String crawlTime = LocalDateTime.now().withNano(0).toString();
        String publishText = publishTime == null ? null
                : publishTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return Parsed.accepted(new JobImportRequest(
                SOURCE,                 // source
                clip(jobId, 160),       // sourceJobId：站点自己的 inn_xxx，作为去重主键
                clip(url, 1200),        // sourceUrl
                title,                  // jobTitle
                clip(normalizedTitle, 100), // normalizedTitle
                company,                // companyName
                city,                   // city
                district,               // district
                salaryText,             // salaryText
                salary.min(),           // salaryMin
                salary.max(),           // salaryMax
                salary.unit(),          // salaryUnit
                salary.months(),        // salaryMonths
                education,              // education
                experience,             // experience
                JOB_TYPE,               // jobType
                description,            // description
                requirements,           // requirements
                null,                   // companyIndustry
                null,                   // companySize
                publishText,            // publishTime
                crawlTime,              // crawlTime
                crawlTime,              // lastSeenTime
                fingerprintValue,       // fingerprint
                contentHash(title, salaryText, description, requirements), // contentHash
                quality,                // qualityScore
                true,                   // isActive
                true,                   // isRecent
                skills));               // skills
    }

    /* ---------------- 解析工具 ---------------- */

    private boolean isAllowedUrl(String url) {
        if (!StringUtils.hasText(url) || !url.startsWith("https://")) {
            return false;
        }
        try {
            String host = java.net.URI.create(url).getHost();
            return host != null && (host.equals(ALLOWED_HOST) || host.endsWith("." + ALLOWED_HOST));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String resolveCity(String locationText, String addressText) {
        if (StringUtils.hasText(locationText)) {
            String[] parts = LOCATION_SPLIT.split(locationText.trim());
            if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                return parts[0];
            }
        }
        if (StringUtils.hasText(addressText)) {
            String[] parts = LOCATION_SPLIT.split(addressText.trim());
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }

    private String resolveDistrict(String addressText, String city) {
        if (!StringUtils.hasText(addressText)) {
            return null;
        }
        for (String part : LOCATION_SPLIT.split(addressText.trim())) {
            String value = part.trim();
            if (value.length() < 2 || value.equals(city)) {
                continue;
            }
            if (value.endsWith("区") || value.endsWith("县") || value.endsWith("市")) {
                return value;
            }
        }
        return null;
    }

    /** 详情页标题格式为「岗位名实习招聘-公司实习生招聘-实习僧」，公司名从中间一段取。 */
    private String companyFromPageTitle(String pageTitle) {
        if (!StringUtils.hasText(pageTitle)) {
            return null;
        }
        String[] parts = pageTitle.split("\\s*[-|｜]\\s*");
        if (parts.length < 2) {
            return null;
        }
        String company = parts[parts.length - 1].contains("实习僧") && parts.length >= 3
                ? parts[parts.length - 2]
                : parts[parts.length - 1];
        return company.replaceAll("实习生招聘|实习招聘|实习生|招聘", "").trim();
    }

    /** 地址串形如「辽宁/大连/甘井子区 蔡大岭软件园29号楼」，只在必要时兜底取公司名。 */
    private String companyFromLocation(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        String[] parts = address.split("\\s+");
        List<String> suffixes = List.of("有限责任公司", "股份有限公司", "有限公司", "集团", "公司", "科技", "电子");
        for (int index = parts.length - 1; index >= 0; index -= 1) {
            String part = parts[index].trim();
            if (suffixes.stream().anyMatch(part::endsWith)) {
                return part;
            }
        }
        return null;
    }

    private LocalDateTime resolvePublishTime(String jobDateText, String pageText) {
        Matcher fromHeader = DATE_TIME.matcher(jobDateText == null ? "" : jobDateText);
        if (fromHeader.find()) {
            LocalDateTime parsed = parseDateTime(fromHeader.group(1));
            if (parsed != null) {
                return parsed;
            }
        }
        Matcher fromPage = DATE_TIME.matcher(pageText == null ? "" : pageText);
        return fromPage.find() ? parseDateTime(fromPage.group(1)) : null;
    }

    private LocalDate resolveDeadline(String pageText) {
        Matcher matcher = DEADLINE.matcher(pageText == null ? "" : pageText);
        if (!matcher.find()) {
            return null;
        }
        try {
            return LocalDate.parse(matcher.group(1).replace('/', '-'), DATE_ONLY);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replace('/', '-').trim();
        try {
            return LocalDateTime.parse(normalized.replace(' ', 'T'));
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(normalized, DATE_ONLY).atStartOfDay();
            } catch (DateTimeParseException alsoIgnored) {
                return null;
            }
        }
    }

    private int firstIndexOf(String text) {
        if (!StringUtils.hasText(text)) {
            return -1;
        }
        int found = -1;
        for (String mark : REQUIREMENT_MARKS) {
            int index = text.indexOf(mark);
            if (index > 0 && (found < 0 || index < found)) {
                found = index;
            }
        }
        return found;
    }

    /** 质量门槛与人工抓取脚本一致，低于 50 分的岗位不进入候选。 */
    private int qualityScore(String title, String company, String city, String salaryText,
                             String description, String requirements, String education,
                             String experience, LocalDateTime publishTime, String url) {
        int body = Math.max(
                description == null ? 0 : description.length(),
                requirements == null ? 0 : requirements.length());
        int score = 0;
        score += StringUtils.hasText(title) ? 15 : 0;
        score += StringUtils.hasText(company) ? 15 : 0;
        score += StringUtils.hasText(city) ? 10 : 0;
        score += StringUtils.hasText(salaryText) ? 15 : 0;
        score += body >= 80 ? 20 : 0;
        score += StringUtils.hasText(education) ? 5 : 0;
        score += StringUtils.hasText(experience) ? 5 : 0;
        score += publishTime != null ? 5 : 0;
        score += StringUtils.hasText(url) ? 10 : 0;
        return Math.min(100, score);
    }

    private ParsedSalary parseSalary(String value) {
        if (!StringUtils.hasText(value) || value.contains("面议")) {
            return new ParsedSalary(null, null, null, null);
        }
        Matcher matcher = SALARY_RANGE.matcher(value.replace(",", ""));
        if (!matcher.find()) {
            return new ParsedSalary(null, null, null, null);
        }
        String unit = matcher.group(3) == null ? "month"
                : matcher.group(3).toLowerCase(Locale.ROOT).replace(" ", "");
        BigDecimal multiplier = unit.startsWith("k") || unit.startsWith("千")
                ? BigDecimal.valueOf(1000)
                : unit.contains("万") ? BigDecimal.valueOf(10000) : BigDecimal.ONE;
        String parsedUnit = unit.contains("天") || unit.contains("日") ? "day"
                : unit.contains("年") ? "year" : "month";
        BigDecimal min = new BigDecimal(matcher.group(1)).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal max = new BigDecimal(matcher.group(2)).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        Integer months = matcher.group(4) == null ? null : Integer.valueOf(matcher.group(4));
        return new ParsedSalary(min, max, parsedUnit, months);
    }

    private String contentHash(String title, String salaryText, String description, String requirements) {
        return sha256(String.join("|",
                title == null ? "" : title,
                salaryText == null ? "" : salaryText,
                description == null ? "" : description,
                requirements == null ? "" : requirements));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String first(Pattern pattern, String html) {
        Matcher matcher = pattern.matcher(html == null ? "" : html);
        return matcher.find() ? matcher.group(1) : "";
    }

    private List<String> all(Pattern pattern, String html) {
        List<String> values = new ArrayList<>();
        Matcher matcher = pattern.matcher(html == null ? "" : html);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    /** 去标签 + 还原实体 + 去掉字体私用区字符，得到可读文本。 */
    private String plainText(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        String text = SCRIPT_OR_STYLE.matcher(html).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = decodeEntities(text);
        text = PRIVATE_USE.matcher(text).replaceAll(" ");
        return text.replaceAll("\\s+", " ").trim();
    }

    private String decodeEntities(String value) {
        Matcher matcher = ENTITY.matcher(value);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String entity = matcher.group();
            String replacement = switch (entity) {
                case "&amp;" -> "&";
                case "&lt;" -> "<";
                case "&gt;" -> ">";
                case "&quot;" -> "\"";
                case "&apos;", "&#39;" -> "'";
                case "&nbsp;" -> " ";
                default -> null;
            };
            if (replacement == null && entity.startsWith("&#")) {
                try {
                    int code = entity.startsWith("&#x") || entity.startsWith("&#X")
                            ? Integer.parseInt(entity.substring(3, entity.length() - 1), 16)
                            : Integer.parseInt(entity.substring(2, entity.length() - 1));
                    replacement = Character.isValidCodePoint(code) ? new String(Character.toChars(code)) : " ";
                } catch (RuntimeException exception) {
                    replacement = " ";
                }
            }
            matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement == null ? " " : replacement));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String clip(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    /** 请求间隔：第一页立即请求，其余按 1.5-3 秒随机等待，避免给对方压力。 */
    private void pause(boolean first) {
        if (first || maxDelayMillis <= 0) {
            return;
        }
        long delay = minDelayMillis >= maxDelayMillis
                ? minDelayMillis
                : ThreadLocalRandom.current().nextLong(minDelayMillis, maxDelayMillis + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RadarStoppedException("UNAVAILABLE", "抓取被中断");
        }
    }

    private record ParsedSalary(BigDecimal min, BigDecimal max, String unit, Integer months) {
    }

    /** 单条详情的解析结果：通过时带记录，未通过时带跳过原因（用于日志统计）。 */
    record Parsed(JobImportRequest request, String skipReason) {

        static Parsed accepted(JobImportRequest request) {
            return new Parsed(request, null);
        }

        static Parsed skipped(String reason) {
            return new Parsed(null, reason);
        }
    }
}
