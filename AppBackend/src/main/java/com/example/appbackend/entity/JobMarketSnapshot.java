package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "job_market_snapshot", uniqueConstraints = @UniqueConstraint(name = "uk_job_market_snapshot", columnNames = {"snapshot_date", "normalized_title", "city", "job_type"}))
public class JobMarketSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
    @Column(name = "normalized_title", nullable = false, length = 100)
    private String normalizedTitle;
    @Column(length = 100)
    private String city;
    @Column(name = "job_type", length = 40)
    private String jobType;
    @Column(name = "active_job_count", nullable = false)
    private long activeJobCount;
    @Column(name = "new_7d_count", nullable = false)
    private long new7dCount;
    @Column(name = "new_30d_count", nullable = false)
    private long new30dCount;
    @Column(name = "salary_p25", precision = 12, scale = 2)
    private BigDecimal salaryP25;
    @Column(name = "salary_p50", precision = 12, scale = 2)
    private BigDecimal salaryP50;
    @Column(name = "salary_p75", precision = 12, scale = 2)
    private BigDecimal salaryP75;
    @Column(name = "intern_ratio", precision = 5, scale = 4)
    private BigDecimal internRatio;
    @Column(name = "campus_ratio", precision = 5, scale = 4)
    private BigDecimal campusRatio;
    @Column(name = "hot_score", precision = 6, scale = 2)
    private BigDecimal hotScore;
    @Lob
    @Column(name = "top_skills_json", columnDefinition = "LONGTEXT")
    private String topSkillsJson;
}
