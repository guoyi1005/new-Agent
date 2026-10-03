package com.example.appbackend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校友企业：实习就业页「校友企业」板块的管理对象。
 * 企业名称、在招状态、开放岗位数等由管理端维护，学生端只读取已展示的企业。
 */
@Data
@Entity
@Table(name = "alumni_enterprise", indexes = {
        @Index(name = "idx_alumni_enterprise_enabled_sort", columnList = "enabled,sort_order")
})
@Schema(description = "校友企业实体")
public class AlumniEnterprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "企业ID", example = "1")
    private Long id;

    @Column(nullable = false, length = 120, columnDefinition = "VARCHAR(120) NOT NULL COMMENT '企业名称'")
    @Schema(description = "企业名称", example = "成都风雨兴科技有限公司")
    private String name;

    @Column(name = "short_name", length = 20, columnDefinition = "VARCHAR(20) COMMENT '企业简称，用于列表徽标'")
    @Schema(description = "企业简称，用于列表徽标", example = "风雨")
    private String shortName;

    @Column(length = 60, columnDefinition = "VARCHAR(60) COMMENT '所属行业'")
    @Schema(description = "所属行业", example = "软件与信息服务")
    private String industry;

    @Column(length = 200, columnDefinition = "VARCHAR(200) COMMENT '招聘方向，多个用逗号分隔'")
    @Schema(description = "招聘方向，多个用逗号分隔", example = "Python,Java,AI")
    private String fields;

    @Column(name = "alumni_count", columnDefinition = "INT DEFAULT 0 COMMENT '本校校友在职人数'")
    @Schema(description = "本校校友在职人数", example = "6")
    private Integer alumniCount = 0;

    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 1 COMMENT '是否正在招聘'")
    @Schema(description = "是否正在招聘", example = "true")
    private Boolean hiring = true;

    @Column(name = "open_positions", columnDefinition = "INT DEFAULT 0 COMMENT '当前开放岗位数'")
    @Schema(description = "当前开放岗位数", example = "3")
    private Integer openPositions = 0;

    @Column(name = "contact_name", length = 60, columnDefinition = "VARCHAR(60) COMMENT '联系人'")
    @Schema(description = "联系人")
    private String contactName;

    @Column(name = "contact_phone", length = 40, columnDefinition = "VARCHAR(40) COMMENT '联系电话'")
    @Schema(description = "联系电话")
    private String contactPhone;

    @Column(length = 1000, columnDefinition = "VARCHAR(1000) COMMENT '企业简介'")
    @Schema(description = "企业简介")
    private String description;

    @Column(name = "sort_order", nullable = false, columnDefinition = "INT DEFAULT 0 COMMENT '排序，值越小越靠前'")
    @Schema(description = "排序，值越小越靠前", example = "0")
    private Integer sortOrder = 0;

    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 1 COMMENT '是否展示: 0-隐藏, 1-展示'")
    @Schema(description = "是否展示", example = "true")
    private Boolean enabled = true;

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
