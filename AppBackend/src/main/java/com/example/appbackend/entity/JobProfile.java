package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位基本信息：岗位方向、类型、职责与发展建议。
 * 与 job_skill_requirement（岗位技能要求）通过 job_code 关联，共同支撑人岗匹配计算。
 */
@Data
@Entity
@Table(name = "job_profile", indexes = {
        @Index(name = "idx_job_profile_code", columnList = "job_code", unique = true)
})
public class JobProfile {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    /** 岗位方向，例如「后端开发 / 服务端」 */
    @Column(length = 80)
    private String direction;

    /** 岗位类型，例如「应届 / 实习」 */
    @Column(name = "job_type", length = 40)
    private String type;

    @Column(length = 200)
    private String summary;

    /** 岗位职责，多条用中文分号分隔 */
    @Column(length = 1000)
    private String responsibilities;

    /** 提升建议 */
    @Column(length = 500)
    private String advice;

    @Column(nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = STATUS_ACTIVE;
        if (sortOrder == null) sortOrder = 0;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
