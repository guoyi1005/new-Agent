package com.example.appbackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 「我的练习」聚合视图：课程进度 + 刷题 + 项目 + 技能增长趋势。 */
@Data
public class LearningPracticeSummaryDTO {

    private List<CourseProgress> courses = new ArrayList<>();
    private ProblemStats problems = new ProblemStats();
    private ProjectStats projects = new ProjectStats();
    private List<SkillLevel> skills = new ArrayList<>();
    private List<TrendPoint> trend = new ArrayList<>();

    @Data
    public static class CourseProgress {
        private Long courseId;
        private String name;
        private Integer progressPercent;
        private Integer completedChapters;
        private Integer totalChapters;
        private List<String> skills = new ArrayList<>();
    }

    @Data
    public static class ProblemStats {
        private int solvedCount;
        private int totalCount;
        private int judgeableCount;
        private int solveRate;
        private int easySolved;
        private int easyTotal;
        private int mediumSolved;
        private int mediumTotal;
        private int hardSolved;
        private int hardTotal;
    }

    @Data
    public static class ProjectStats {
        private int total;
        private int completed;
        private int inProgress;
        private List<ProjectItem> items = new ArrayList<>();
    }

    @Data
    public static class ProjectItem {
        private Long itemId;
        private String title;
        private String status;
        private List<String> skills = new ArrayList<>();
    }

    @Data
    public static class SkillLevel {
        private String code;
        private String name;
        private String category;
        private Integer level;
        private Integer evidenceCount;
    }

    @Data
    public static class TrendPoint {
        private String date;
        private Integer averageLevel;
        private Integer eventCount;
    }
}
