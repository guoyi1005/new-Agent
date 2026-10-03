package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "market_job_skill", indexes = {
        @Index(name = "idx_market_job_skill_job", columnList = "job_id"),
        @Index(name = "idx_market_job_skill_normalized", columnList = "normalized_skill")
}, uniqueConstraints = @UniqueConstraint(name = "uk_market_job_skill", columnNames = {"job_id", "normalized_skill"}))
public class MarketJobSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "job_id", nullable = false)
    private Long jobId;
    @Column(name = "skill_name", nullable = false, length = 80)
    private String skillName;
    @Column(name = "normalized_skill", nullable = false, length = 80)
    private String normalizedSkill;
    @Column(name = "skill_category", length = 40)
    private String skillCategory;
}
