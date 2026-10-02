package com.example.appbackend.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * 抓取招聘网站页面用的轻量 HTTP 客户端：统一 UA、超时与 Referer。
 * 只做单次 GET/POST，不跟随站内其它接口，尽量减少对方压力。
 */
@Component
public class LocalJobFetcher {

    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private final WebClient webClient;
    private final Duration timeout;

    public LocalJobFetcher(
            WebClient.Builder webClientBuilder,
            @Value("${local-job.crawl.user-agent:" + DEFAULT_USER_AGENT + "}") String userAgent,
            @Value("${local-job.crawl.timeout-seconds:25}") long timeoutSeconds) {
        this.webClient = webClientBuilder.clone()
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9")
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/json,application/xhtml+xml,*/*;q=0.8")
                .build();
        this.timeout = Duration.ofSeconds(Math.max(5, timeoutSeconds));
    }

    public String get(String url, String referer) {
        return webClient.get()
                .uri(url)
                .headers(headers -> addReferer(headers, referer))
                .retrieve()
                .bodyToMono(String.class)
                .block(timeout);
    }

    public String postForm(String url, MultiValueMap<String, String> form, String referer) {
        return webClient.post()
                .uri(url)
                .headers(headers -> {
                    addReferer(headers, referer);
                    headers.add("X-Requested-With", "XMLHttpRequest");
                })
                .bodyValue(form)
                .retrieve()
                .bodyToMono(String.class)
                .block(timeout);
    }

    private void addReferer(HttpHeaders headers, String referer) {
        if (referer != null && !referer.isBlank()) {
            headers.add(HttpHeaders.REFERER, referer);
        }
    }
}
