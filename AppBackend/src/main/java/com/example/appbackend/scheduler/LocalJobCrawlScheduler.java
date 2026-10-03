package com.example.appbackend.scheduler;

import com.example.appbackend.service.LocalJobIngestService;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 本地就业岗位的抓取调度：每天固定时间更新一次，
 * 另外在服务启动后如果库里还没有数据，补跑一次，避免页面空着。
 */
@Service
public class LocalJobCrawlScheduler {

    private static final Logger log = LoggerFactory.getLogger(LocalJobCrawlScheduler.class);

    private final LocalJobIngestService localJobIngestService;

    private final int staleHours;

    public LocalJobCrawlScheduler(
            LocalJobIngestService localJobIngestService,
            @Value("${local-job.crawl.stale-hours:12}") int staleHours) {
        this.localJobIngestService = localJobIngestService;
        this.staleHours = staleHours;
    }

    @Scheduled(cron = "${local-job.crawl.cron:0 30 5 * * ?}")
    public void refreshDaily() {
        run("每日定时抓取");
    }

    @Scheduled(
            initialDelayString = "${local-job.crawl.bootstrap-delay-ms:60000}",
            fixedDelayString = "${local-job.crawl.bootstrap-interval-ms:3600000}")
    public void bootstrapWhenEmpty() {
        if (!localJobIngestService.needsRefresh(staleHours)) {
            return;
        }
        run("数据过期补抓");
    }

    private void run(String reason) {
        try {
            LocalJobIngestService.RefreshResult result = localJobIngestService.refreshAll();
            log.info("{}完成：抓取 {} 条，新增 {} 条，更新 {} 条", reason, result.fetched(), result.created(), result.updated());
        } catch (Exception exception) {
            log.warn("{}失败：{}", reason, exception.getMessage());
        }
    }
}
