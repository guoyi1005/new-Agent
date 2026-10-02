package com.example.appbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成都本地就业岗位（来自公共招聘网站的公开职位信息）。
 *
 * <p>只保存招聘网站上公开展示的岗位字段，不抓取联系人、电话等个人信息。
 * 同一岗位可能在多个平台同时发布，用 fingerprint（岗位名 + 单位 + 区域）去重，
 * 因此 source 记录的是首次收录该岗位的来源。</p>
 */
@Data
@Entity
@Table(
        name = "local_job_postings",
        indexes = {
                @Index(name = "idx_local_job_fingerprint", columnList = "fingerprint", unique = true),
                @Index(name = "idx_local_job_published", columnList = "published_at"),
                @Index(name = "idx_local_job_district", columnList = "district"),
                @Index(name = "idx_local_job_category", columnList = "job_category")
        }
)
public class LocalJobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "source_key", nullable = false, length = 32)
    private String sourceKey;

    @Column(name = "source_name", nullable = false, length = 64)
    private String sourceName;

    /** 同一岗位出现过的所有平台（逗号分隔），用于体现「多平台聚合」。 */
    @Column(name = "source_keys", length = 255)
    private String sourceKeys;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(name = "job_title", nullable = false, length = 160)
    private String jobTitle;

    @Column(name = "company", length = 160)
    private String company;

    @Column(name = "city", length = 32)
    private String city;

    @Column(name = "district", length = 32)
    private String district;

    @Column(name = "salary_text", length = 64)
    private String salaryText;

    @Column(name = "education", length = 32)
    private String education;

    /** intern 实习 / campus 校招 / stateOwned 国企 / other 其它，按岗位名与单位名关键词粗分。 */
    @Column(name = "job_category", nullable = false, length = 24)
    private String jobCategory;

    @Column(name = "detail_url", columnDefinition = "LONGTEXT")
    private String detailUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (firstSeenAt == null) {
            firstSeenAt = now;
        }
        lastSeenAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        lastSeenAt = LocalDateTime.now();
    }
}
