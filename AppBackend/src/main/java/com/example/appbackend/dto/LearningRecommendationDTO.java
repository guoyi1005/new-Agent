package com.example.appbackend.dto;

import lombok.Data;

/** 统一推荐结果：把岗位技能差距映射到具体的课程/题目/项目/专项内容。 */
@Data
public class LearningRecommendationDTO {
    private String sourceType;
    private Long sourceId;
    private String title;
    /** 外部课程的学习链接；仅 EXTERNAL_COURSE 类型有值 */
    private String url;
    private String skillCode;
    private String skillName;
    private Integer requiredLevel;
    private Integer currentLevel;
    private Integer gap;
    private String reason;
    private Double priority;

    /** 以下字段仅 PROJECT（岗位实战任务）类型有值 */
    private String objective;
    private String deliverable;
    private String difficulty;
    private Integer estimatedHours;
}