package com.example.appbackend.service;

import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseChapter;
import com.example.appbackend.entity.CampusCourseEnrollment;
import com.example.appbackend.entity.CampusCourseProgress;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningRecord;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.entity.User;
import com.example.appbackend.repository.CampusCourseChapterRepository;
import com.example.appbackend.repository.CampusCourseEnrollmentRepository;
import com.example.appbackend.repository.CampusCourseProgressRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningRecordRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.repository.PythonProblemRepository;
import com.example.appbackend.repository.UserRepository;
import com.example.appbackend.util.SkillTextMatcher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 「我的练习」演示学习数据。
 *
 * 仅用于明确开启的演示环境。默认关闭，正式账号不能被自动补入虚构学习轨迹。
 *
 * 约束：
 * 1. 只在用户「没有任何学习记录」时生成；已有学习记录说明账号在用，一律跳过；
 * 2. 记录的事件 ID 由「用户 + 内容 + 序号」确定，重复执行不会重复写入；
 * 3. 时间基于当天向前推算，保证趋势图始终落在最近三周；
 * 4. 仅设置 app.demo-learning-data.enabled=true 时启用。
 */
@Service
public class LearningDemoDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(LearningDemoDataSeeder.class);

    private static final String SOURCE_COURSE = "COURSE";
    private static final String SOURCE_PROBLEM = "PROBLEM";
    private static final String ACTION_COURSE = "COURSE_PROGRESS";
    private static final String ACTION_PROBLEM = "PROBLEM_SOLVED";

    /** 生成学习轨迹的课程，按完成度从高到低。 */
    private static final List<CoursePlan> COURSE_PLANS = List.of(
            new CoursePlan("Python 编程基础", 70),
            new CoursePlan("MySQL 数据库基础", 45),
            new CoursePlan("前端开发基础", 25)
    );

    /** 每个技能写入三个时间点（距今天数）与对应等级，形成上升曲线。 */
    private static final int[] SKILL_DAYS = {18, 11, 4};
    private static final int[] SKILL_LEVELS = {38, 58, 76};

    /** 生成多少道已通过的算法题。 */
    private static final int DEMO_SOLVED_PROBLEMS = 9;

    @Value("${app.demo-learning-data.enabled:false}")
    private boolean enabled;

    private final LearningRecordRepository recordRepository;
    private final CampusCourseEnrollmentRepository enrollmentRepository;
    private final CampusCourseProgressRepository progressRepository;
    private final CampusCourseRepository courseRepository;
    private final CampusCourseChapterRepository chapterRepository;
    private final LearningContentSkillRepository contentSkillRepository;
    private final LearningSkillRepository skillRepository;
    private final PythonProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public LearningDemoDataSeeder(
            LearningRecordRepository recordRepository,
            CampusCourseEnrollmentRepository enrollmentRepository,
            CampusCourseProgressRepository progressRepository,
            CampusCourseRepository courseRepository,
            CampusCourseChapterRepository chapterRepository,
            LearningContentSkillRepository contentSkillRepository,
            LearningSkillRepository skillRepository,
            PythonProblemRepository problemRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.recordRepository = recordRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepository = progressRepository;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.contentSkillRepository = contentSkillRepository;
        this.skillRepository = skillRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public boolean enabled() {
        return enabled;
    }

    /** 用户没有任何学习记录时生成一份演示轨迹；已有记录一律跳过，不覆盖真实学习数据。 */
    @Transactional
    public boolean seedIfEmpty(Long userId) {
        if (!enabled || userId == null) return false;
        if (!recordRepository.findByUserIdOrderByOccurredAtDesc(userId).isEmpty()) return false;
        seedCourses(userId);
        seedProblems(userId);
        log.info("已为账号 {} 生成「我的练习」演示学习数据", userId);
        return true;
    }

    /** 启动时为所有学生账号补齐演示数据，队友拉取代码重启后端即可看到。 */
    @Transactional
    public int seedAllStudents() {
        if (!enabled) return 0;
        List<User> students = userRepository
                .findByConditions(null, "STUDENT", 1, PageRequest.of(0, 500))
                .getContent();
        int created = 0;
        for (User student : students) {
            if (seedIfEmpty(student.getId())) created++;
        }
        return created;
    }

    private void seedCourses(Long userId) {
        for (CoursePlan plan : COURSE_PLANS) {
            CampusCourse course = findPublishedCourse(plan.courseName());
            if (course == null) continue;
            List<CampusCourseChapter> chapters =
                    chapterRepository.findByCourseIdOrderBySortOrderAscIdAsc(course.getId());
            if (chapters.isEmpty()) continue;
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, course.getId())) {
                CampusCourseEnrollment enrollment = new CampusCourseEnrollment();
                enrollment.setUserId(userId);
                enrollment.setCourseId(course.getId());
                enrollmentRepository.save(enrollment);
            }
            // 该课程已有真实进度时不再改动，只补技能记录
            boolean hasProgress = !progressRepository
                    .findByCourseIdAndUserId(course.getId(), userId).isEmpty();
            int completed = Math.max(1, Math.round(chapters.size() * plan.percent() / 100f));
            for (int i = 0; !hasProgress && i < completed && i < chapters.size(); i++) {
                CampusCourseChapter chapter = chapters.get(i);
                CampusCourseProgress progress = progressRepository
                        .findByCourseIdAndChapterIdAndUserId(course.getId(), chapter.getId(), userId)
                        .orElseGet(CampusCourseProgress::new);
                progress.setCourseId(course.getId());
                progress.setChapterId(chapter.getId());
                progress.setUserId(userId);
                progress.setCompleted(Boolean.TRUE);
                if (progress.getCompletedTime() == null) {
                    progress.setCompletedTime(LocalDateTime.now().minusDays(Math.max(1, 19 - i)));
                }
                progressRepository.save(progress);
            }
            for (LearningContentSkill link
                    : contentSkillRepository.findBySourceTypeAndSourceId(SOURCE_COURSE, course.getId())) {
                if (link.getSkillId() == null) continue;
                for (int i = 0; i < SKILL_DAYS.length; i++) {
                    String eventId = eventId("course", userId, course.getId(), link.getSkillId(), i);
                    if (recordRepository.existsByEventId(eventId)) continue;
                    LearningRecord record = new LearningRecord();
                    record.setUserId(userId);
                    record.setSkillId(link.getSkillId());
                    record.setSourceType(SOURCE_COURSE);
                    record.setSourceId(course.getId());
                    record.setActionType(ACTION_COURSE);
                    record.setProgress(SKILL_LEVELS[i]);
                    record.setScore((double) SKILL_LEVELS[i]);
                    record.setEvidence("学习《" + course.getName() + "》阶段 " + (i + 1)
                            + "，技能进度 " + SKILL_LEVELS[i] + " 分");
                    record.setEventId(eventId);
                    record.setOccurredAt(LocalDateTime.now().minusDays(SKILL_DAYS[i]));
                    recordRepository.save(record);
                }
            }
        }
    }

    private void seedProblems(Long userId) {
        List<PythonProblem> ordered = new ArrayList<>();
        List<PythonProblem> problems = problemRepository.findByEnabledTrueOrderByNumberAsc();
        for (PythonProblem problem : problems) {
            if ("easy".equalsIgnoreCase(problem.getDifficulty())) ordered.add(problem);
        }
        for (PythonProblem problem : problems) {
            if (!"easy".equalsIgnoreCase(problem.getDifficulty())) ordered.add(problem);
        }
        int count = 0;
        int day = 15;
        for (PythonProblem problem : ordered) {
            if (count >= DEMO_SOLVED_PROBLEMS) break;
            String eventId = eventId("problem", userId, problem.getId(), 0, 0);
            if (!recordRepository.existsByEventId(eventId)) {
                LearningRecord record = new LearningRecord();
                record.setUserId(userId);
                record.setSkillId(resolveProblemSkillId(problem));
                record.setSourceType(SOURCE_PROBLEM);
                record.setSourceId(problem.getId());
                record.setActionType(ACTION_PROBLEM);
                record.setProgress(72);
                record.setScore(100d);
                record.setEvidence("通过在线判题：" + problem.getTitle());
                record.setEventId(eventId);
                record.setOccurredAt(LocalDateTime.now().minusDays(Math.max(1, day)));
                recordRepository.save(record);
            }
            count++;
            day = Math.max(1, day - 2);
        }
    }

    private Long resolveProblemSkillId(PythonProblem problem) {
        for (String code : SkillTextMatcher.skillsForProblemTags(readTags(problem.getTags()))) {
            LearningSkill skill = skillRepository.findByCode(code).orElse(null);
            if (skill != null) return skill.getId();
        }
        return null;
    }

    private CampusCourse findPublishedCourse(String name) {
        for (CampusCourse course : courseRepository.findByPublishStatusOrderBySortOrderAscPublishTimeDesc(
                CampusCourse.STATUS_PUBLISHED)) {
            if (name.equals(course.getName())) return course;
        }
        return null;
    }

    private String eventId(String kind, Long userId, Long sourceId, long skillId, int index) {
        return "demo-" + kind + "-" + userId + "-" + sourceId + "-" + skillId + "-" + index;
    }

    private List<String> readTags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private record CoursePlan(String courseName, int percent) { }
}
