package com.example.appbackend.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;

/**
 * 岗位雷达抓取用的 HTTP 客户端。
 *
 * <p>只对公开的列表页与职位详情页做单次 GET，不调用站内接口、不带账号、不携带 Cookie，
 * 请求之间由抓取器控制间隔。遇到 403/429 或验证页时直接判定为「被拦截」并结束本次抓取，
 * 不做任何绕过（不换 IP、不模拟登录、不解验证码、不研究字体混淆）。</p>
 */
@Component
public class MarketJobRadarFetcher {

    /** 出现这些字样说明站点返回了验证/登录页，本次抓取必须停止。 */
    private static final String[] BLOCK_MARKERS = {
            "访问验证", "滑动验证", "请先登录", "security verification", "verify you are human"
    };

    private final WebClient webClient;
    private final Duration timeout;

    public MarketJobRadarFetcher(
            WebClient.Builder webClientBuilder,
            @Value("${market-job.radar.user-agent:CampusJobResearch/1.0}") String userAgent,
            @Value("${market-job.radar.timeout-seconds:25}") long timeoutSeconds) {
        this.webClient = webClientBuilder.clone()
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.5")
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,*/*;q=0.8")
                .build();
        this.timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
    }

    public String get(String url) {
        String body;
        try {
            body = webClient.get()
                    .uri(url)
                    .exchangeToMono(response -> {
                        int status = response.statusCode().value();
                        if (status == 403 || status == 429) {
                            return Mono.error(new RadarStoppedException("BLOCKED",
                                    "来源返回 HTTP " + status + "，本次抓取停止"));
                        }
                        if (status >= 400) {
                            return Mono.error(new RadarStoppedException("UNAVAILABLE",
                                    "来源返回 HTTP " + status));
                        }
                        return response.bodyToMono(String.class);
                    })
                    .block(timeout);
        } catch (RadarStoppedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RadarStoppedException("UNAVAILABLE", "请求失败：" + exception.getMessage());
        }
        if (body == null || body.isBlank()) {
            throw new RadarStoppedException("UNAVAILABLE", "来源返回空页面");
        }
        String lower = body.toLowerCase(Locale.ROOT);
        for (String marker : BLOCK_MARKERS) {
            if (lower.contains(marker.toLowerCase(Locale.ROOT))) {
                throw new RadarStoppedException("BLOCKED", "检测到验证/登录页，本次抓取停止");
            }
        }
        return body;
    }

    /** 被来源拦截或来源不可用：整轮抓取立即停止并如实报告原因。 */
    public static class RadarStoppedException extends RuntimeException {

        private final String status;

        public RadarStoppedException(String status, String message) {
            super(message);
            this.status = status;
        }

        public String status() {
            return status;
        }
    }
}
