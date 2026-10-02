package com.example.appbackend.scheduler;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 定时抓取的 cron 必须是「每天固定时间跑一次」。
 * 这个用例把表达式解析出来，直接断言它的触发规律，不依赖真实调度器。
 */
class LocalJobCrawlCronTest {

    private static final String CRON = "0 30 5 * * ?";

    @Test
    void dailyCronFiresOncePerDayAt0530() {
        CronExpression expression = CronExpression.parse(CRON);
        LocalDateTime from = LocalDateTime.of(2026, 10, 2, 12, 0);

        LocalDateTime first = expression.next(from);
        LocalDateTime second = expression.next(first);
        LocalDateTime third = expression.next(second);

        assertEquals(LocalDateTime.of(2026, 10, 3, 5, 30), first);
        assertEquals(LocalDateTime.of(2026, 10, 4, 5, 30), second);
        assertEquals(LocalDateTime.of(2026, 10, 5, 5, 30), third);
        assertTrue(first.equals(second.minusDays(1)) && second.equals(third.minusDays(1)));
    }
}
