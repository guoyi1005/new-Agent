package com.example.appbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端学习内容配置：技能字典、岗位技能要求、岗位实战任务。
 * 这三张表直接决定学生端「推荐学习」算出什么。
 */
public class AdminLearningTaxonomyDTO {

    @Data
    public static class SkillView {
        private Long id;
        private String code;
        private String name;
        private String category;
        private String description;
        private Integer sortOrder;
        private String status;
        private long jobRequirementCount;
        private long contentUsageCount;
    }

    @Data
    public static class SkillRequest {
        @NotBlank(message = "技能编码不能为空")
        @Size(max = 80)
        private String code;

        @NotBlank(message = "技能名称不能为空")
        @Size(max = 120)
        private String name;

        @Size(max = 80)
        private String category;

        @Size(max = 500)
        private String description;

        private Integer sortOrder = 0;

        private String status;
    }

    @Data
    public static class SkillOption {
        private Long id;
        private String code;
        private String name;
        private String category;
    }

    @Data
    public static class JobOption {
        private String code;
        private String name;
        private long skillCount;
    }

    @Data
    public static class RequirementView {
        private Long id;
        private String jobCode;
        private String jobName;
        private Long skillId;
        private String skillCode;
        private String skillName;
        private Integer requiredLevel;
        private Double importance;
        private Boolean requiredFlag;
        private Integer sortOrder;
    }

    @Data
    public static class RequirementRequest {
        @NotBlank(message = "岗位编码不能为空")
        private String jobCode;

        @NotBlank(message = "岗位名称不能为空")
        private String jobName;

        @NotNull(message = "请选择技能")
        private Long skillId;

        private Integer requiredLevel;

        private Double importance;

        private Boolean requiredFlag;

        private Integer sortOrder;
    }

    @Data
    public static class ProjectView {
        private Long id;
        private String code;
        private String title;
        private String summary;
        private String objective;
        private String deliverable;
        private String difficulty;
        private Integer estimatedHours;
        private Integer sortOrder;
        private String status;
        private List<Long> skillIds = new ArrayList<>();
        private List<String> skills = new ArrayList<>();
        private List<String> jobs = new ArrayList<>();
    }

    @Data
    public static class ProjectRequest {
        @NotBlank(message = "任务编码不能为空")
        @Size(max = 80)
        private String code;

        @NotBlank(message = "任务标题不能为空")
        @Size(max = 160)
        private String title;

        @Size(max = 500)
        private String summary;

        @Size(max = 500)
        private String objective;

        @Size(max = 300)
        private String deliverable;

        @Size(max = 20)
        private String difficulty;

        private Integer estimatedHours;

        private Integer sortOrder = 0;

        private String status;

        private List<Long> skillIds = new ArrayList<>();
    }
}
