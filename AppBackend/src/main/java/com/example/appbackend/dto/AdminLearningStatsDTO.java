package com.example.appbackend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 管理端：学生端学习行为的汇总看板。 */
public class AdminLearningStatsDTO {

    @Data
    public static class Overview {
        private long learningRecordCount;
        private long activeStudentCount;
        private long enrollmentCount;
        private long enrolledStudentCount;
        private long examAttemptCount;
        private long examStudentCount;
        private double averageExamScoreRate;
        private long studyGoalCount;
        private long studyGoalInProgress;
        private long studyGoalCompleted;
        private double averageGoalProgress;
        private List<SourceBreakdown> sourceBreakdown = new ArrayList<>();
        private Map<String, Long> actionBreakdown = new LinkedHashMap<>();
    }

    @Data
    public static class SourceBreakdown {
        private String sourceType;
        private long recordCount;
        private long studentCount;
    }
}
