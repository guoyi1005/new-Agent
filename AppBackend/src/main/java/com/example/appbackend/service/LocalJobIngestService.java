package com.example.appbackend.service;

import com.example.appbackend.entity.LocalJobPosting;
import com.example.appbackend.repository.LocalJobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 本地岗位入库：把各抓取器的结果去重后写入 local_job_postings。
 *
 * <p>去重键是「岗位名 + 单位 + 区域」的哈希，同一岗位在多个平台发布只会保留一条，
 * 并在 sourceKeys 里记下出现过的平台。单个来源失败只记录日志，不影响其它来源。</p>
 */
@Service
public class LocalJobIngestService {

    private static final Logger log = LoggerFactory.getLogger(LocalJobIngestService.class);

    private static final List<String> INTERN_WORDS = List.of("实习", "见习", "兼职", "假期工");
    private static final List<String> CAMPUS_WORDS = List.of("校招", "应届", "管培", "校园", "储备干部", "双选会");
    private static final List<String> STATE_OWNED_WORDS = List.of(
            "国企", "国有", "集团有限公司", "城投", "交投", "建投", "水务", "供电", "燃气", "公交",
            "中国烟草", "国家电网", "铁路局", "航空集团", "能源集团", "投资集团", "发展集团", "控股集团");

    private final List<LocalJobCrawler> crawlers;
    private final LocalJobPostingRepository repository;

    public LocalJobIngestService(List<LocalJobCrawler> crawlers, LocalJobPostingRepository repository) {
        this.crawlers = crawlers;
        this.repository = repository;
    }

    public boolean hasData() {
        return repository.count() > 0;
    }

    /**
     * 是否需要补抓：库里没有数据，或者最近一次抓取已经超过 staleHours 小时。
     * 这样即使某天 05:30 服务没在运行，下次启动后也会自动补上。
     */
    public boolean needsRefresh(int staleHours) {
        if (repository.count() == 0) {
            return true;
        }
        LocalDateTime lastCrawledAt = repository.findLastCrawledAt();
        return lastCrawledAt == null
                || lastCrawledAt.isBefore(LocalDateTime.now().minusHours(Math.max(1, staleHours)));
    }

    /**
     * 跑一遍所有启用的来源。
     *
     * @return 本次抓取结果统计，供日志与手动刷新接口返回
     */
    @Transactional
    public RefreshResult refreshAll() {
        int fetched = 0;
        int created = 0;
        int updated = 0;
        List<String> sources = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        reclassifyExisting();

        for (LocalJobCrawler crawler : crawlers) {
            if (!crawler.enabled()) {
                continue;
            }
            try {
                List<LocalJobCrawler.CrawledJob> jobs = crawler.crawl();
                fetched += jobs.size();
                sources.add(crawler.sourceName() + " " + jobs.size() + " 条");
                for (LocalJobCrawler.CrawledJob job : jobs) {
                    if (upsert(crawler, job)) {
                        created++;
                    } else {
                        updated++;
                    }
                }
            } catch (Exception exception) {
                failures.add(crawler.sourceName() + "：" + exception.getMessage());
                log.warn("抓取 {} 失败：{}", crawler.sourceName(), exception.getMessage());
            }
        }

        log.info("本地岗位抓取完成：抓取 {} 条，新增 {} 条，更新 {} 条，失败 {} 个来源", fetched, created, updated, failures.size());
        return new RefreshResult(fetched, created, updated, sources, failures, LocalDateTime.now());
    }

    /** 分类规则调整后，把已有数据按同一套规则重算一遍，避免历史数据停留在旧口径。 */
    private void reclassifyExisting() {
        List<LocalJobPosting> postings = repository.findAll();
        List<LocalJobPosting> changed = new ArrayList<>();
        for (LocalJobPosting posting : postings) {
            String category = classify(posting.getJobTitle(), posting.getCompany());
            if (!category.equals(posting.getJobCategory())) {
                posting.setJobCategory(category);
                changed.add(posting);
            }
        }
        if (!changed.isEmpty()) {
            repository.saveAll(changed);
            log.info("按最新规则修正了 {} 条岗位分类", changed.size());
        }
    }

    private boolean upsert(LocalJobCrawler crawler, LocalJobCrawler.CrawledJob job) {
        String fingerprint = fingerprintOf(job);
        Optional<LocalJobPosting> existing = repository.findByFingerprint(fingerprint);
        if (existing.isEmpty()) {
            LocalJobPosting posting = new LocalJobPosting();
            posting.setFingerprint(fingerprint);
            posting.setSourceKey(crawler.sourceKey());
            posting.setSourceName(crawler.sourceName());
            posting.setSourceKeys(crawler.sourceKey());
            posting.setExternalId(job.externalId());
            posting.setJobTitle(job.jobTitle());
            posting.setCompany(job.company());
            posting.setCity(job.city());
            posting.setDistrict(job.district());
            posting.setSalaryText(job.salaryText());
            posting.setEducation(job.education());
            posting.setJobCategory(categoryOf(job));
            posting.setDetailUrl(job.detailUrl());
            posting.setPublishedAt(job.publishedAt());
            repository.save(posting);
            return true;
        }

        LocalJobPosting posting = existing.get();
        posting.setSourceKeys(mergeSources(posting.getSourceKeys(), crawler.sourceKey()));
        if (!StringUtils.hasText(posting.getSalaryText())) {
            posting.setSalaryText(job.salaryText());
        }
        if (!StringUtils.hasText(posting.getEducation())) {
            posting.setEducation(job.education());
        }
        if (!StringUtils.hasText(posting.getDistrict())) {
            posting.setDistrict(job.district());
        }
        if (!StringUtils.hasText(posting.getDetailUrl())) {
            posting.setDetailUrl(job.detailUrl());
        }
        if (posting.getPublishedAt() == null || (job.publishedAt() != null && job.publishedAt().isAfter(posting.getPublishedAt()))) {
            posting.setPublishedAt(job.publishedAt());
        }
        // 分类是按岗位名 / 单位名推导出来的，每次抓取都重算，规则调整后历史数据也会跟着更新
        posting.setJobCategory(categoryOf(job));
        repository.save(posting);
        return false;
    }

    /** 关键词补抓带回来的分类优先，其次才按岗位名和单位名判断。 */
    private static String categoryOf(LocalJobCrawler.CrawledJob job) {
        String hint = job.categoryHint();
        return StringUtils.hasText(hint) ? hint : classify(job.jobTitle(), job.company());
    }

    private static String mergeSources(String existing, String added) {
        Set<String> keys = new LinkedHashSet<>();
        if (StringUtils.hasText(existing)) {
            keys.addAll(List.of(existing.split(",")));
        }
        keys.add(added);
        return String.join(",", keys);
    }

    /** 岗位名 + 单位 + 区域，作为跨平台去重键。 */
    static String fingerprintOf(LocalJobCrawler.CrawledJob job) {
        String raw = normalize(job.jobTitle()) + "|" + normalize(job.company()) + "|" + normalize(job.district());
        return sha256(raw).substring(0, 40);
    }

    /**
     * 实习 / 校招 / 国企按关键词粗分，只是给首页统计用的近似分类，
     * 不改变岗位本身的信息。
     */
    static String classify(String jobTitle, String company) {
        String title = jobTitle == null ? "" : jobTitle;
        String unit = company == null ? "" : company;
        if (containsAny(title, INTERN_WORDS)) {
            return "intern";
        }
        if (containsAny(title, CAMPUS_WORDS)) {
            return "campus";
        }
        if (containsAny(unit, STATE_OWNED_WORDS)) {
            return "stateOwned";
        }
        return "other";
    }

    private static boolean containsAny(String value, List<String> words) {
        for (String word : words) {
            if (value.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s（）()·、,，/]+", "").toLowerCase();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算岗位去重键", exception);
        }
    }

    public record RefreshResult(
            int fetched,
            int created,
            int updated,
            List<String> sources,
            List<String> failures,
            LocalDateTime finishedAt
    ) {
    }
}
