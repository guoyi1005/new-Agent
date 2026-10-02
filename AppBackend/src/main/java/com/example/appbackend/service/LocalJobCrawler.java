package com.example.appbackend.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一个本地招聘网站的抓取器。每个实现负责把站点上的公开职位整理成统一结构，
 * 新增站点只需要再实现一个 bean，抓取与入库逻辑不用改。
 */
public interface LocalJobCrawler {

    String sourceKey();

    String sourceName();

    default boolean enabled() {
        return true;
    }

    /** 抓取失败时抛出异常，由上层记录并跳过该来源，不影响其它来源。 */
    List<CrawledJob> crawl();

    /** 招聘网站上公开展示的岗位字段，不含任何联系方式。 */
    record CrawledJob(
            String externalId,
            String jobTitle,
            String company,
            String city,
            String district,
            String salaryText,
            String education,
            String detailUrl,
            LocalDateTime publishedAt,
            /** 招聘网站自己标注的岗位类别（多数站点为空）。 */
            String categoryHint
    ) {
    }
}
