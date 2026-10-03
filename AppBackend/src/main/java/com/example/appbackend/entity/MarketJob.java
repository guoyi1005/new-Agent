package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "market_job", indexes = {
        @Index(name = "idx_market_job_status_type_title", columnList = "status, job_type, normalized_title"),
        @Index(name = "idx_market_job_fingerprint", columnList = "fingerprint", unique = true),
        @Index(name = "idx_market_job_last_seen", columnList = "last_seen_time")
})
public class MarketJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String source;
    @Column(name = "source_job_id", length = 160)
    private String sourceJobId;
    @Column(name = "source_url", nullable = false, length = 1200)
    private String sourceUrl;
    @Column(name = "job_title", nullable = false, length = 240)
    private String jobTitle;
    @Column(name = "normalized_title", nullable = false, length = 100)
    private String normalizedTitle;
    @Column(name = "job_category", length = 100)
    private String jobCategory;
    @Column(name = "company_name", nullable = false, length = 240)
    private String companyName;
    @Column(name = "company_industry", length = 160)
    private String companyIndustry;
    @Column(name = "company_size", length = 80)
    private String companySize;
    @Column(name = "financing_stage", length = 80)
    private String financingStage;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String district;
    @Column(name = "job_type", nullable = false, length = 40)
    private String jobType;
    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;
    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;
    @Column(name = "salary_unit", length = 24)
    private String salaryUnit;
    @Column(name = "salary_months")
    private Integer salaryMonths;
    @Column(length = 60)
    private String education;
    @Column(length = 80)
    private String experience;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String requirements;
    @Column(name = "publish_time")
    private LocalDateTime publishTime;
    @Column(name = "source_update_time")
    private LocalDateTime sourceUpdateTime;
    @Column(name = "crawl_time", nullable = false)
    private LocalDateTime crawlTime;
    @Column(name = "last_seen_time", nullable = false)
    private LocalDateTime lastSeenTime;
    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";
    @Column(nullable = false, length = 64)
    private String fingerprint;
    @Column(name = "content_hash", length = 64)
    private String contentHash;
    @Column(name = "salary_text", length = 120)
    private String salaryText;
    @Column(name = "quality_score")
    private Integer qualityScore;
}
