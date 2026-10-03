package com.example.appbackend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 校园招聘：实习就业页「校园招聘」板块的活动与岗位条目。
 * 类型分为宣讲会、双选会和校招岗位，学生端据此统计场次与岗位数。
 */
@Data
@Entity
@Table(name = "campus_recruitment", indexes = {
        @Index(name = "idx_campus_recruitment_status_sort", columnList = "status,sort_order")
})
@Schema(description = "校园招聘实体")
public class CampusRecruitment {

    public static final String TYPE_TALK = "TALK";
    public static final String TYPE_FAIR = "FAIR";
    public static final String TYPE_JOB = "JOB";

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "条目ID", example = "1")
    private Long id;

    @Column(nullable = false, length = 160, columnDefinition = "VARCHAR(160) NOT NULL COMMENT '活动或岗位名称'")
    @Schema(description = "活动或岗位名称", example = "企业宣讲")
    private String title;

    @Column(length = 120, columnDefinition = "VARCHAR(120) COMMENT '主办单位或招聘企业'")
    @Schema(description = "主办单位或招聘企业")
    private String company;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL COMMENT '类型: TALK-宣讲会, FAIR-双选会, JOB-校招岗位'")
    @Schema(description = "类型: TALK-宣讲会, FAIR-双选会, JOB-校招岗位", example = "TALK")
    private String type = TYPE_TALK;

    @Column(length = 40, columnDefinition = "VARCHAR(40) COMMENT '招聘季，如 2027 届秋招'")
    @Schema(description = "招聘季", example = "2027 届秋招")
    private String season;

    @Column(length = 40, columnDefinition = "VARCHAR(40) COMMENT '城市'")
    @Schema(description = "城市", example = "成都")
    private String city;

    @Column(length = 160, columnDefinition = "VARCHAR(160) COMMENT '活动地点'")
    @Schema(description = "活动地点")
    private String location;

    @Column(name = "event_date", columnDefinition = "DATE COMMENT '活动日期'")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "活动日期", example = "2026-10-08")
    private LocalDate eventDate;

    @Column(name = "role_count", columnDefinition = "INT DEFAULT 0 COMMENT '校招岗位数'")
    @Schema(description = "校招岗位数", example = "12")
    private Integer roleCount = 0;

    @Column(length = 1000, columnDefinition = "VARCHAR(1000) COMMENT '说明'")
    @Schema(description = "说明")
    private String description;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL COMMENT '状态: DRAFT-草稿, PUBLISHED-已发布'")
    @Schema(description = "状态: DRAFT-草稿, PUBLISHED-已发布", example = "PUBLISHED")
    private String status = STATUS_PUBLISHED;

    @Column(name = "sort_order", nullable = false, columnDefinition = "INT DEFAULT 0 COMMENT '排序，值越小越靠前'")
    @Schema(description = "排序，值越小越靠前", example = "0")
    private Integer sortOrder = 0;

    @Column(name = "create_time", columnDefinition = "DATETIME COMMENT '创建时间'")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Column(name = "update_time", columnDefinition = "DATETIME COMMENT '更新时间'")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
