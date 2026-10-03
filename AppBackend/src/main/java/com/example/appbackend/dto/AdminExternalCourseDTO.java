package com.example.appbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 管理端外部精选课程的请求与视图。 */
public class AdminExternalCourseDTO {

    @Data
    public static class Request {
        @NotBlank(message = "课程标题不能为空")
        @Size(max = 160)
        private String title;

        @NotBlank(message = "来源平台不能为空")
        @Size(max = 80)
        private String provider;

        @NotBlank(message = "学习链接不能为空")
        @Size(max = 500)
        private String url;

        @Size(max = 600)
        private String description;

        @Size(max = 20)
        private String level;

        private Boolean free = true;

        private Integer sortOrder = 0;

        private String status;

        private List<Long> skillIds = new ArrayList<>();
    }

    @Data
    public static class View {
        private Long id;
        private String title;
        private String provider;
        private String url;
        private String description;
        private String level;
        private Boolean free;
        private Integer sortOrder;
        private String status;
        private LocalDateTime createdAt;
        private List<Long> skillIds = new ArrayList<>();
        private List<String> skills = new ArrayList<>();
        private List<String> jobs = new ArrayList<>();
    }

    /** 技能下拉选项。 */
    public record SkillOption(Long id, String name, String category) {
    }
}
