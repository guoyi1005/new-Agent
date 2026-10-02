package com.example.appbackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 内容标签：课程 / 算法题 / 项目节点统一使用的技能、岗位、难度标签。 */
@Data
public class ContentTagDTO {
    private String sourceType;
    private Long sourceId;
    private String title;
    private String difficulty;
    private Integer recommendationOrder;
    private List<SkillTag> skills = new ArrayList<>();
    private List<SkillTag> prerequisites = new ArrayList<>();
    private List<JobTag> jobs = new ArrayList<>();

    @Data
    public static class SkillTag {
        private Long skillId;
        private String code;
        private String name;
        private Boolean primary;
        private Integer targetLevel;
    }

    @Data
    public static class JobTag {
        private String code;
        private String name;
    }
}