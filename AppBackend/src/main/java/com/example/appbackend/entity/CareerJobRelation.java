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
 * 岗位关系：职业路径图谱里星球与星球之间的连线配置。
 *
 * 只描述「岗位之间的关系」，不重复保存技能数据：
 * 可复用技能与待补齐技能默认由 job_skill_requirement + learning_record 自动算出，
 * 管理员也可以在这里手工覆盖（字段留空即走自动计算）。
 */
@Data
@Entity
@Table(name = "career_job_relation", indexes = {
        @Index(name = "idx_career_job_relation_source", columnList = "source_job_id"),
        @Index(name = "idx_career_job_relation_target", columnList = "target_job_id")
})
public class CareerJobRelation {

    /** 职业进阶 */
    public static final String TYPE_PROMOTION = "PROMOTION";
    /** 横向转岗 */
    public static final String TYPE_TRANSFER = "TRANSFER";
    /** 相近岗位 */
    public static final String TYPE_RELATED = "RELATED";
    /** 发展分支 */
    public static final String TYPE_BRANCH = "BRANCH";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 源岗位：对应星图里的星球 id */
    @Column(name = "source_job_id", nullable = false, length = 80)
    private String sourceJobId;

    /** 目标岗位：对应星图里的星球 id */
    @Column(name = "target_job_id", nullable = false, length = 80)
    private String targetJobId;

    /** 关系类型：PROMOTION / TRANSFER / RELATED / BRANCH */
    @Column(name = "relation_type", nullable = false, length = 20)
    private String relationType = TYPE_RELATED;

    /** 关系名称，例如「横向转岗」，留空时按关系类型给默认文案 */
    @Column(name = "relation_name", length = 40)
    private String relationName;

    /** 关系说明 */
    @Column(length = 400)
    private String description;

    /** 可复用技能，留空则自动计算 */
    @Column(name = "reusable_skills", length = 400)
    private String reusableSkills;

    /** 建议新增技能，留空则自动计算 */
    @Column(name = "missing_skills", length = 400)
    private String missingSkills;

    /** 推荐程度 0-100，只影响排序与标签，不影响星球大小/坐标 */
    @Column(nullable = false)
    private Integer recommendation = 60;

    /** 连线类型：SOLID / DASHED / THIN / GLOW */
    @Column(name = "line_type", nullable = false, length = 20)
    private String lineType = "DASHED";

    @Column(nullable = false)
    private Boolean enabled = true;

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
        if (relationType == null || relationType.isBlank()) relationType = TYPE_RELATED;
        if (lineType == null || lineType.isBlank()) lineType = "DASHED";
        if (enabled == null) enabled = true;
        if (sortOrder == null) sortOrder = 0;
        if (recommendation == null) recommendation = 60;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
