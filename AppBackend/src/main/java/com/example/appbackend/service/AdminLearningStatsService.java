package com.example.appbackend.service;

import com.example.appbackend.dto.AdminLearningStatsDTO.Overview;
import com.example.appbackend.dto.AdminLearningStatsDTO.SourceBreakdown;
import com.example.appbackend.entity.CampusCourseEnrollment;
import com.example.appbackend.entity.ExamPaperAttempt;
import com.example.appbackend.entity.LearningRecord;
import com.example.appbackend.entity.StudyGoal;
import com.example.appbackend.repository.CampusCourseEnrollmentRepository;
import com.example.appbackend.repository.ExamPaperAttemptRepository;
import com.example.appbackend.repository.LearningRecordRepository;
import com.example.appbackend.repository.StudyGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 管理端：学生学习行为汇总。
 *
 * <p>只做读取聚合，不修改任何学生数据。当前数据量为演示规模，
 * 因此直接读取后在内存里汇总；后续数据量上来后再改成数据库聚合查询。</p>
 */
@Service
public class AdminLearningStatsService {

    private final LearningRecordRepository learningRecordRepository;
    private final CampusCourseEnrollmentRepository enrollmentRepository;
    private final ExamPaperAttemptRepository attemptRepository;
    private final StudyGoalRepository studyGoalRepository;

    public AdminLearningStatsService(LearningRecordRepository learningRecordRepository,
                                     CampusCourseEnrollmentRepository enrollmentRepository,
                                     ExamPaperAttemptRepository attemptRepository,
                                     StudyGoalRepository studyGoalRepository) {
        this.learningRecordRepository = learningRecordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attemptRepository = attemptRepository;
        this.studyGoalRepository = studyGoalRepository;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        Overview view = new Overview();

        List<LearningRecord> records = learningRecordRepository.findAll();
        view.setLearningRecordCount(records.size());
        Set<Long> recordStudents = new HashSet<>();
        Map<String, long[]> bySource = new TreeMap<>();
        Map<String, Set<Long>> studentsBySource = new LinkedHashMap<>();
        Map<String, Long> byAction = new TreeMap<>();
        for (LearningRecord record : records) {
            if (record.getUserId() != null) recordStudents.add(record.getUserId());
            String sourceType = record.getSourceType() == null ? "UNKNOWN" : record.getSourceType();
            bySource.computeIfAbsent(sourceType, key -> new long[1])[0]++;
            if (record.getUserId() != null) {
                studentsBySource.computeIfAbsent(sourceType, key -> new HashSet<>()).add(record.getUserId());
            }
            String actionType = record.getActionType() == null ? "UNKNOWN" : record.getActionType();
            byAction.merge(actionType, 1L, Long::sum);
        }
        view.setActiveStudentCount(recordStudents.size());
        for (Map.Entry<String, long[]> entry : bySource.entrySet()) {
            SourceBreakdown breakdown = new SourceBreakdown();
            breakdown.setSourceType(entry.getKey());
            breakdown.setRecordCount(entry.getValue()[0]);
            breakdown.setStudentCount(studentsBySource.getOrDefault(entry.getKey(), Set.of()).size());
            view.getSourceBreakdown().add(breakdown);
        }
        view.setActionBreakdown(byAction);

        List<CampusCourseEnrollment> enrollments = enrollmentRepository.findAll();
        view.setEnrollmentCount(enrollments.size());
        Set<Long> enrolledStudents = new HashSet<>();
        for (CampusCourseEnrollment enrollment : enrollments) {
            if (enrollment.getUserId() != null) enrolledStudents.add(enrollment.getUserId());
        }
        view.setEnrolledStudentCount(enrolledStudents.size());

        BigDecimal score = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        long attemptCount = 0;
        Set<Long> examStudents = new HashSet<>();
        for (ExamPaperAttempt attempt : attemptRepository.findAll()) {
            if (attempt.getStatus() == ExamPaperAttempt.Status.IN_PROGRESS) continue;
            attemptCount++;
            if (attempt.getUserId() != null) examStudents.add(attempt.getUserId());
            if (attempt.getObjectiveScore() != null) score = score.add(attempt.getObjectiveScore());
            if (attempt.getObjectiveTotalScore() != null) total = total.add(attempt.getObjectiveTotalScore());
        }
        view.setExamAttemptCount(attemptCount);
        view.setExamStudentCount(examStudents.size());
        view.setAverageExamScoreRate(total.signum() == 0
                ? 0d
                : score.multiply(BigDecimal.valueOf(100))
                        .divide(total, 1, java.math.RoundingMode.HALF_UP)
                        .doubleValue());

        List<StudyGoal> goals = studyGoalRepository.findAll();
        view.setStudyGoalCount(goals.size());
        long completed = 0;
        long inProgress = 0;
        long progressSum = 0;
        for (StudyGoal goal : goals) {
            String status = goal.getStatus() == null ? "" : goal.getStatus().toLowerCase();
            if ("completed".equals(status) || "done".equals(status)) completed++;
            else if ("in_progress".equals(status) || "active".equals(status) || "pending".equals(status)) inProgress++;
            progressSum += goal.getProgress() == null ? 0 : goal.getProgress();
        }
        view.setStudyGoalCompleted(completed);
        view.setStudyGoalInProgress(inProgress);
        view.setAverageGoalProgress(goals.isEmpty()
                ? 0d
                : Math.round(progressSum * 10.0 / goals.size()) / 10.0);

        return view;
    }
}
