package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外部精选课程：只保存标题、来源、简介与官方学习链接，学习时跳转到原站，
 * 不复制对方的视频/讲义，避免版权风险。
 */
@Data
@Entity
@Table(name = "external_course", indexes = {
        @Index(name = "idx_external_course_status", columnList = "status,sort_order")
})
public class ExternalCourse {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_OFFLINE = "OFFLINE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 600)
    private String description;

    @Column(length = 20)
    private String level;

    @Column(name = "is_free", nullable = false)
    private Boolean free = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
        if (free == null) free = true;
        if (sortOrder == null) sortOrder = 0;
        if (status == null) status = STATUS_ACTIVE;
    }
}