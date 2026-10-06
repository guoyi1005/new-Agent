package com.example.appbackend.scheduler;

import com.example.appbackend.service.MarketJobRadarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 岗位雷达调度：每天固定时间抓一次实习岗位，
 * 服务启动后如果库里没有在招实习岗位（或数据已过期），补跑一次，避免就业页空着。
 */
@Service
public class MarketJobRadarScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketJobRadarScheduler.class);

    private final MarketJobRadarService radarService;

    public MarketJobRadarScheduler(MarketJobRadarService radarService) {
        this.radarService = radarService;
    }

    @Scheduled(cron = "${market-job.radar.cron:0 40 5 * * ?}")
    public void refreshDaily() {
        run("每日岗位雷达抓取");
    }

    @Scheduled(
            initialDelayString = "${market-job.radar.bootstrap-delay-ms:120000}",
            fixedDelayString = "${market-job.radar.bootstrap-interval-ms:3600000}")
    public void bootstrapWhenEmpty() {
        if (!radarService.needsRefresh()) {
            return;
        }
        run("岗位雷达数据为空补抓");
    }

    private void run(String reason) {
        MarketJobRadarService.RefreshResult result = radarService.refresh();
        if (!result.success()) {
            log.warn("{}未完成：{}", reason, result.message());
            return;
        }
        log.info("{}完成：候选 {} 条，新增 {} 条，更新 {} 条，跳过 {} 条",
                reason, result.fetched(), result.inserted(), result.updated(), result.skipped());
    }
}
