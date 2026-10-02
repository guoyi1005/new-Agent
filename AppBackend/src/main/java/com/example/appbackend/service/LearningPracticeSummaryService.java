package com.example.appbackend.service;

import com.example.appbackend.dto.LearningPracticeSummaryDTO;
import com.example.appbackend.dto.LearningPathDTO;
import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseChapter;
import com.example.appbackend.entity.CampusCourseProgress;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningRecord;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.repository.CampusCourseChapterRepository;
import com.example.appbackend.repository.CampusCourseEnrollmentRepository;
import com.example.appbackend.repository.CampusCourseProgressRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningRecordRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.repository.PythonProblemRepository;
import com.example.appbackend.util.SkillTextMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 「我的练习」聚合：把课程学习进度、刷题记录、项目节点与技能等级汇总成一个视图，
 * 全部基于统一学习记录与真实业务表，不做演示数据填充。
 */
@Service
public class LearningPracticeSummaryService {

    private static final String SOURCE_COURSE = "COURSE";
    private static final String SOURCE_PROBLEM = "PROBLEM";
    private static final String PYTHON = "python";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_TREND_POINTS = 30;

    private final CampusCourseEnrollmentRepository enrollmentRepository;
    private final CampusCourseRepository courseRepository;
    private final CampusCourseChapterRepository chapterRepository;
    private final CampusCourseProgressRepository progressRepository;
    private final LearningContentSkillRepository contentSkillRepository;
    private final LearningSkillRepository skillRepository;
    private final LearningRecordRepository recordRepository;
    private final PythonProblemRepository problemRepository;
    private final LearningPathService learningPathService;

    public LearningPracticeSummaryService(
            CampusCourseEnrollmentRepository enrollmentRepository,
            CampusCourseRepository courseRepository,
            CampusCourseChapterRepository chapterRepository,
            CampusCourseProgressRepository progressRepository,
            LearningContentSkillRepository contentSkillRepository,
            LearningSkillRepository skillRepository,
            LearningRecordRepository recordRepository,
            PythonProblemRepository problemRepository,
            LearningPathService learningPathService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.progressRepository = progressRepository;
        this.contentSkillRepository = contentSkillRepository;
        this.skillRepository = skillRepository;
        this.recordRepository = recordRepository;
        this.problemRepository = problemRepository;
        this.learningPathService = learningPathService;
    }

    @Transactional(readOnly = true)
    public LearningPracticeSummaryDTO summary(Long userId) {
        LearningPracticeSummaryDTO view = new LearningPracticeSummaryDTO();
        if (userId == null) return view;
        Map<String, LearningSkill> skillByCode = allSkillsByCode();
        Map<Long, LearningSkill> skillById = allSkillsById();
        List<LearningRecord> records = recordRepository.findByUserIdOrderByOccurredAtDesc(userId);

        view.setCourses(courses(userId, skillById));
        view.setProblems(problems(records));
        view.setProjects(projects(userId, skillByCode));
        view.setSkills(skills(records, skillById));
        view.setTrend(trend(records, skillById));
        return view;
    }

    private List<LearningPracticeSummaryDTO.CourseProgress> courses(Long userId,
                                                                    Map<Long, LearningSkill> skillById) {
        List<LearningPracticeSummaryDTO.CourseProgress> result = new ArrayList<>();
        for (var enrollment : enrollmentRepository.findByUserIdOrderByEnrolledTimeDesc(userId)) {
            CampusCourse course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course == null) continue;
            List<CampusCourseChapter> chapters =
                    chapterRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId());
            Set<Long> completed = new LinkedHashSet<>();
            for (CampusCourseProgress progress : progressRepository.findByCourseIdAndUserId(course.getId(), userId)) {
                if (Boolean.TRUE.equals(progress.getCompleted())) completed.add(progress.getChapterId());
            }
            LearningPracticeSummaryDTO.CourseProgress item = new LearningPracticeSummaryDTO.CourseProgress();
            item.setCourseId(course.getId());
            item.setName(course.getName());
            item.setTotalChapters(chapters.size());
            item.setCompletedChapters(completed.size());
            item.setProgressPercent(chapters.isEmpty() ? 0
                    : (int) Math.round(completed.size() * 100.0 / chapters.size()));
            for (LearningContentSkill link
                    : contentSkillRepository.findBySourceTypeAndSourceId(SOURCE_COURSE, course.getId())) {
                LearningSkill skill = link.getSkillId() == null ? null : skillById.get(link.getSkillId());
                if (skill != null && !item.getSkills().contains(skill.getName())) {
                    item.getSkills().add(skill.getName());
                }
            }
            result.add(item);
        }
        return result;
    }

    private LearningPracticeSummaryDTO.ProblemStats problems(List<LearningRecord> records) {
        LearningPracticeSummaryDTO.ProblemStats stats = new LearningPracticeSummaryDTO.ProblemStats();
        List<PythonProblem> problems = problemRepository.findByEnabledTrueOrderByNumberAsc();
        Set<Long> solved = new LinkedHashSet<>();
        for (LearningRecord record : records) {
            if (SOURCE_PROBLEM.equals(record.getSourceType()) && record.getSourceId() != null) {
                solved.add(record.getSourceId());
            }
        }
        int judgeable = 0;
        for (PythonProblem problem : problems) {
            boolean canJudge = problem.getFuncName() != null && !problem.getFuncName().isBlank()
                    && problem.getTestcases() != null && !problem.getTestcases().isBlank();
            if (canJudge) judgeable++;
            boolean done = solved.contains(problem.getId());
            switch (problem.getDifficulty() == null ? "" : problem.getDifficulty().toLowerCase()) {
                case "easy" -> {
                    stats.setEasyTotal(stats.getEasyTotal() + 1);
                    if (done) stats.setEasySolved(stats.getEasySolved() + 1);
                }
                case "hard" -> {
                    stats.setHardTotal(stats.getHardTotal() + 1);
                    if (done) stats.setHardSolved(stats.getHardSolved() + 1);
                }
                default -> {
                    stats.setMediumTotal(stats.getMediumTotal() + 1);
                    if (done) stats.setMediumSolved(stats.getMediumSolved() + 1);
                }
            }
        }
        stats.setTotalCount(problems.size());
        stats.setJudgeableCount(judgeable);
        stats.setSolvedCount(solved.size());
        stats.setSolveRate(judgeable == 0 ? 0 : (int) Math.round(solved.size() * 100.0 / judgeable));
        return stats;
    }

    private LearningPracticeSummaryDTO.ProjectStats projects(Long userId, Map<String, LearningSkill> skillByCode) {
        LearningPracticeSummaryDTO.ProjectStats stats = new LearningPracticeSummaryDTO.ProjectStats();
        LearningPathDTO.PathView path;
        try {
            path = learningPathService.getActivePath(userId, PYTHON);
        } catch (RuntimeException ignored) {
            return stats;
        }
        if (path == null || path.getItems() == null) return stats;
        for (LearningPathDTO.PathItemView item : path.getItems()) {
            LearningPracticeSummaryDTO.ProjectItem row = new LearningPracticeSummaryDTO.ProjectItem();
            row.setItemId(item.getId());
            row.setTitle(item.getKnowledgePoint());
            row.setStatus(item.getStatus());
            String text = (item.getKnowledgePoint() == null ? "" : item.getKnowledgePoint())
                    + " " + (item.getObjective() == null ? "" : item.getObjective());
            for (String code : SkillTextMatcher.matchText(text)) {
                LearningSkill skill = skillByCode.get(code);
                if (skill != null) row.getSkills().add(skill.getName());
            }
            stats.getItems().add(row);
            stats.setTotal(stats.getTotal() + 1);
            if ("completed".equals(item.getStatus())) stats.setCompleted(stats.getCompleted() + 1);
            else if ("in_progress".equals(item.getStatus())) stats.setInProgress(stats.getInProgress() + 1);
        }
        return stats;
    }

    private List<LearningPracticeSummaryDTO.SkillLevel> skills(List<LearningRecord> records,
                                                               Map<Long, LearningSkill> skillById) {
        Map<Long, Integer> levels = levelsBySkill(records);
        List<LearningPracticeSummaryDTO.SkillLevel> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : levels.entrySet()) {
            LearningSkill skill = skillById.get(entry.getKey());
            if (skill == null) continue;
            LearningPracticeSummaryDTO.SkillLevel row = new LearningPracticeSummaryDTO.SkillLevel();
            row.setCode(skill.getCode());
            row.setName(skill.getName());
            row.setCategory(skill.getCategory());
            row.setLevel(entry.getValue());
            result.add(row);
        }
        result.sort((left, right) -> Integer.compare(right.getLevel(), left.getLevel()));
        return result;
    }

    /** 技能增长趋势：按天重放学习记录，取当天各技能等级的平均值。 */
    private List<LearningPracticeSummaryDTO.TrendPoint> trend(List<LearningRecord> records,
                                                              Map<Long, LearningSkill> skillById) {
        Map<LocalDate, List<LearningRecord>> byDay = new TreeMap<>();
        for (LearningRecord record : records) {
            LocalDateTime at = record.getOccurredAt() == null ? record.getCreatedAt() : record.getOccurredAt();
            if (at == null) continue;
            byDay.computeIfAbsent(at.toLocalDate(), key -> new ArrayList<>()).add(record);
        }
        List<LearningPracticeSummaryDTO.TrendPoint> result = new ArrayList<>();
        Map<Long, Integer> cumulative = new HashMap<>();
        for (Map.Entry<LocalDate, List<LearningRecord>> entry : byDay.entrySet()) {
            for (LearningRecord record : entry.getValue()) {
                if (record.getSkillId() == null) continue;
                int progress = record.getProgress() == null ? 0 : record.getProgress();
                cumulative.merge(record.getSkillId(), progress, Math::max);
            }
            LearningPracticeSummaryDTO.TrendPoint point = new LearningPracticeSummaryDTO.TrendPoint();
            point.setDate(entry.getKey().format(DATE));
            point.setEventCount(entry.getValue().size());
            point.setAverageLevel(averageLevel(cumulative, skillById));
            result.add(point);
        }
        if (result.size() > MAX_TREND_POINTS) {
            return new ArrayList<>(result.subList(result.size() - MAX_TREND_POINTS, result.size()));
        }
        return result;
    }

    private int averageLevel(Map<Long, Integer> levels, Map<Long, LearningSkill> skillById) {
        int total = 0;
        int count = 0;
        for (Map.Entry<Long, Integer> entry : levels.entrySet()) {
            if (!skillById.containsKey(entry.getKey())) continue;
            total += entry.getValue();
            count++;
        }
        return count == 0 ? 0 : Math.round((float) total / count);
    }

    private Map<Long, Integer> levelsBySkill(List<LearningRecord> records) {
        Map<Long, Integer> levels = new LinkedHashMap<>();
        for (LearningRecord record : records) {
            if (record.getSkillId() == null) continue;
            int progress = record.getProgress() == null ? 0 : record.getProgress();
            levels.merge(record.getSkillId(), progress, Math::max);
        }
        return levels;
    }

    private Map<String, LearningSkill> allSkillsByCode() {
        Map<String, LearningSkill> map = new HashMap<>();
        for (LearningSkill skill : skillRepository.findAll()) {
            map.put(skill.getCode(), skill);
        }
        return map;
    }

    private Map<Long, LearningSkill> allSkillsById() {
        Map<Long, LearningSkill> map = new HashMap<>();
        for (LearningSkill skill : skillRepository.findAll()) {
            map.put(skill.getId(), skill);
        }
        return map;
    }
}