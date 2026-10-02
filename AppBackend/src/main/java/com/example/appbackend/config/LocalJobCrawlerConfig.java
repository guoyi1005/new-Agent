package com.example.appbackend.config;

import com.example.appbackend.service.LocalJobCrawler;
import com.example.appbackend.service.impl.LocalJobFetcher;
import com.example.appbackend.service.impl.Sc91JobCrawler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地就业岗位抓取来源登记处：要增减网站，只改这里和 application.yml。
 * 目前接入成都人才网、成都公共招聘网、四川公共招聘网（成都岗位）。
 */
@Configuration
public class LocalJobCrawlerConfig {

    @Bean
    public LocalJobCrawler chengduPublicJobCrawler(
            LocalJobFetcher fetcher,
            ObjectMapper objectMapper,
            @Value("${local-job.crawl.cd-sc91.enabled:${local-job.crawl.enabled:true}}") boolean enabled,
            @Value("${local-job.crawl.cd-sc91.pages:3}") int pages,
            @Value("${local-job.crawl.pause-millis:400}") long pauseMillis,
            @Value("${local-job.crawl.direction-keywords:Python,Java,前端,数据}") String directionKeywords) {
        return new Sc91JobCrawler(
                fetcher,
                objectMapper,
                "cd-sc91",
                "成都公共招聘网",
                "https://cd.sc91.org.cn",
                enabled,
                true,
                pages,
                pauseMillis,
                directionKeywords);
    }

    @Bean
    public LocalJobCrawler sichuanPublicJobCrawler(
            LocalJobFetcher fetcher,
            ObjectMapper objectMapper,
            @Value("${local-job.crawl.sc91.enabled:${local-job.crawl.enabled:true}}") boolean enabled,
            @Value("${local-job.crawl.sc91.pages:3}") int pages,
            @Value("${local-job.crawl.pause-millis:400}") long pauseMillis,
            @Value("${local-job.crawl.direction-keywords:Python,Java,前端,数据}") String directionKeywords) {
        return new Sc91JobCrawler(
                fetcher,
                objectMapper,
                "sc91",
                "四川公共招聘网",
                "https://www.sc91.org.cn",
                enabled,
                true,
                pages,
                pauseMillis,
                directionKeywords);
    }
}
