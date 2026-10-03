package com.example.appbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位实战任务：把一项技能落成可交付的实战项目。
 * 通过 learning_content_skill(source_type=PROJECT) 与技能建立多对多关联，
 * 从而能随目标岗位的技能要求一起被推荐。
 */
@Data
@Entity
@Table(name = "learning_project", indexes = {
        @Index(name = "idx_learning_project_status", columnList = "status")
})
public class LearningProject {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_OFFLINE = "OFFLINE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String summary;

    /** 任务目标：做完要达到什么效果 */
    @Column(length = 500)
    private String objective;

    /** 交付物：做完要产出什么 */
    @Column(length = 300)
    private String deliverable;

    /** BEGINNER / INTERMEDIATE / ADVANCED */
    @Column(length = 20)
    private String difficulty;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
