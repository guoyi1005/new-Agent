package com.example.appbackend.service;

import com.example.appbackend.dto.JobImportRequest;
import com.example.appbackend.entity.MarketJob;
import com.example.appbackend.repository.MarketJobRepository;
import com.example.appbackend.service.impl.MarketJobRadarFetcher.RadarStoppedException;
import com.example.appbackend.service.impl.ShixisengJobRadarCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 岗位雷达：每天抓取一次实习岗位并写入 market_job。
 *
 * <p>写库复用 {@link MarketJobImportService}，因此质量门槛、去重、热门快照刷新与
 * 人工导入走的是同一套逻辑；「为你推荐的岗位」和「热门岗位」两个板块都读这份数据。</p>
 */
@Service
public class MarketJobRadarService {

    private static final Logger log = LoggerFactory.getLogger(MarketJobRadarService.class);
    private static final String SOURCE = "shixiseng";
    private static final String JOB_TYPE = "internship";
    private static final String STATUS_ACTIVE = "ACTIVE";
    /** 单次导入的分批大小，避免一个事务里塞太多行。 */
    private static final int IMPORT_BATCH_SIZE = 100;

    private final ShixisengJobRadarCrawler crawler;
    private final MarketJobImportService importService;
    private final MarketJobRepository repository;

    private final boolean enabled;
    private final List<String> keywords;
    private final int perKeywordLimit;
    private final int staleHours;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public MarketJobRadarService(
            ShixisengJobRadarCrawler crawler,
            MarketJobImportService importService,
            MarketJobRepository repository,
            @Value("${market-job.radar.enabled:true}") boolean enabled,
            @Value("${market-job.radar.keywords:Python,Java,前端,数据分析,产品经理,大模型}") String keywords,
            @Value("${market-job.radar.per-keyword-limit:12}") int perKeywordLimit,
            @Value("${market-job.radar.stale-hours:24}") int staleHours) {
        this.crawler = crawler;
        this.importService = importService;
        this.repository = repository;
        this.enabled = enabled;
        this.perKeywordLimit = Math.max(1, perKeywordLimit);
        this.staleHours = Math.max(1, staleHours);
        this.keywords = new ArrayList<>();
        for (String item : keywords.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                this.keywords.add(trimmed);
            }
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean running() {
        return running.get();
    }

    /** 库里没有在招实习岗位，或最近一次抓取已经超过 staleHours 小时。 */
    public boolean needsRefresh() {
        if (!enabled) {
            return false;
        }
        if (repository.countByStatusAndJobTypeIgnoreCase(STATUS_ACTIVE, JOB_TYPE) == 0) {
            return true;
        }
        LocalDateTime lastCrawledAt = lastCrawlTime();
        return lastCrawledAt == null || lastCrawledAt.isBefore(LocalDateTime.now().minusHours(staleHours));
    }

    public LocalDateTime lastCrawlTime() {
        return repository.findFirstByOrderByCrawlTimeDesc().map(MarketJob::getCrawlTime).orElse(null);
    }

    public Map<String, Object> status() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("enabled", enabled);
        payload.put("running", running.get());
        payload.put("keywords", keywords);
        payload.put("perKeywordLimit", perKeywordLimit);
        payload.put("activeInternshipJobs", repository.countByStatusAndJobTypeIgnoreCase(STATUS_ACTIVE, JOB_TYPE));
        payload.put("lastCrawlTime", lastCrawlTime());
        return payload;
    }

    /**
     * 跑一轮抓取。
     *
     * @param overrideKeywords 手动触发时可临时指定关键词，为空时用配置里的方向
     * @param overrideLimit    手动触发时可临时指定每个关键词的条数
     */
    public RefreshResult refresh(List<String> overrideKeywords, Integer overrideLimit) {
        if (!enabled) {
            return new RefreshResult(false, "岗位雷达已关闭（market-job.radar.enabled=false）",
                    0, 0, 0, 0, LocalDateTime.now());
        }
        if (!running.compareAndSet(false, true)) {
            return new RefreshResult(false, "已有一次岗位雷达抓取正在进行", 0, 0, 0, 0, LocalDateTime.now());
        }
        try {
            List<String> keywordsToUse = overrideKeywords == null || overrideKeywords.isEmpty()
                    ? keywords : overrideKeywords;
            int limit = overrideLimit == null ? perKeywordLimit : Math.max(1, overrideLimit);
            List<JobImportRequest> requests = crawler.crawl(keywordsToUse, limit);
            int inserted = 0;
            int updated = 0;
            int skipped = 0;
            for (int offset = 0; offset < requests.size(); offset += IMPORT_BATCH_SIZE) {
                List<JobImportRequest> batch = requests.subList(
                        offset, Math.min(requests.size(), offset + IMPORT_BATCH_SIZE));
                MarketJobImportService.ImportResult result = importService.importBatch(batch);
                inserted += result.inserted();
                updated += result.updated();
                skipped += result.skipped();
            }
            log.info("岗位雷达抓取完成：候选 {} 条，新增 {} 条，更新 {} 条，跳过 {} 条",
                    requests.size(), inserted, updated, skipped);
            return new RefreshResult(true, "抓取完成", requests.size(), inserted, updated, skipped, LocalDateTime.now());
        } catch (RadarStoppedException exception) {
            log.warn("岗位雷达被来源拦截或不可用：{} - {}", exception.status(), exception.getMessage());
            return new RefreshResult(false, exception.getMessage(), 0, 0, 0, 0, LocalDateTime.now());
        } catch (RuntimeException exception) {
            log.warn("岗位雷达抓取失败：{}", exception.getMessage());
            return new RefreshResult(false, "抓取失败：" + exception.getMessage(), 0, 0, 0, 0, LocalDateTime.now());
        } finally {
            running.set(false);
        }
    }

    public RefreshResult refresh() {
        return refresh(null, null);
    }

    public record RefreshResult(
            boolean success,
            String message,
            int fetched,
            int inserted,
            int updated,
            int skipped,
            LocalDateTime finishedAt
    ) {
    }
}
