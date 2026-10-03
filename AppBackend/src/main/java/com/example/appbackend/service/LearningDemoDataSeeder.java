package com.example.appbackend.service;

import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseChapter;
import com.example.appbackend.entity.CampusCourseEnrollment;
import com.example.appbackend.entity.CampusCourseProgress;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningKnowledgeMastery;
import com.example.appbackend.entity.LearningPath;
import com.example.appbackend.entity.LearningPathItem;
import com.example.appbackend.entity.LearningRecord;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.ExamPaper;
import com.example.appbackend.entity.ExamPaperAttempt;
import com.example.appbackend.entity.ExamPaperAttemptAnswer;
import com.example.appbackend.entity.ExamPaperQuestion;
import com.example.appbackend.entity.PythonPaper;
import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.entity.User;
import com.example.appbackend.repository.CampusCourseChapterRepository;
import com.example.appbackend.repository.CampusCourseEnrollmentRepository;
import com.example.appbackend.repository.CampusCourseProgressRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningKnowledgeMasteryRepository;
import com.example.appbackend.repository.LearningPathItemRepository;
import com.example.appbackend.repository.LearningPathRepository;
import com.example.appbackend.repository.LearningRecordRepository;
import com.example.appbackend.repository.ExamPaperAttemptAnswerRepository;
import com.example.appbackend.repository.ExamPaperAttemptRepository;
import com.example.appbackend.repository.ExamPaperQuestionRepository;
import com.example.appbackend.repository.ExamPaperRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.repository.PythonPaperRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final String PYTHON_COURSE_KEY = "python";

    /**
     * 知识图谱的 7 个目录知识点，与 PythonKnowledgeGraphServiceImpl 的 CATALOG 一致。
     * 状态刻意做出梯度（已掌握 / 需巩固 / 学习中 / 待解锁），图谱才不会是一片"待解锁"。
     */
    private static final List<MasterySeed> MASTERY_SEEDS = List.of(
            new MasterySeed("python.expression.arithmetic", "算术表达式", "mastered", 86, 6, 5, 1),
            new MasterySeed("python.data_type.collection", "集合类型", "weak", 48, 4, 2, 2),
            new MasterySeed("python.data_type.sequence", "序列类型", "weak", 42, 3, 1, 2),
            new MasterySeed("python.function.syntax", "函数定义", "learning", 58, 2, 1, 1),
            new MasterySeed("python.exception.application", "异常处理实践", "new", 0, 0, 0, 0),
            new MasterySeed("python.exception.reliability", "异常与可靠性", "new", 0, 0, 0, 0),
            new MasterySeed("python.algorithm.complexity", "算法复杂度", "new", 0, 0, 0, 0));

    /** 学习路径环节：使用知识点 key，前端会用统一展示名渲染成中文。 */
    private static final List<PathItemSeed> PATH_ITEM_SEEDS = List.of(
            new PathItemSeed("python.expression.arithmetic", "巩固算术表达式的运算顺序与常见写法", "completed"),
            new PathItemSeed("python.data_type.collection", "复习 list、dict、set 的常用操作与选型", "needs_review"),
            new PathItemSeed("python.data_type.sequence", "复习列表与元组的可变性差异", "needs_review"),
            new PathItemSeed("python.function.syntax", "掌握 def、参数传递与返回值", "in_progress"),
            new PathItemSeed("python.exception.application", "练习 try / except 的基本用法", "ready"),
            new PathItemSeed("python.exception.reliability", "理解恢复、友好提示与精确捕获", "locked"),
            new PathItemSeed("python.algorithm.complexity", "学会判断循环次数与时间复杂度", "locked"));

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
    private final ExamPaperRepository examPaperRepository;
    private final ExamPaperQuestionRepository examPaperQuestionRepository;
    private final ExamPaperAttemptRepository attemptRepository;
    private final ExamPaperAttemptAnswerRepository attemptAnswerRepository;
    private final PythonPaperRepository pythonPaperRepository;
    private final LearningKnowledgeMasteryRepository masteryRepository;
    private final LearningPathRepository pathRepository;
    private final LearningPathItemRepository pathItemRepository;
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
            ExamPaperRepository examPaperRepository,
            ExamPaperQuestionRepository examPaperQuestionRepository,
            ExamPaperAttemptRepository attemptRepository,
            ExamPaperAttemptAnswerRepository attemptAnswerRepository,
            PythonPaperRepository pythonPaperRepository,
            LearningKnowledgeMasteryRepository masteryRepository,
            LearningPathRepository pathRepository,
            LearningPathItemRepository pathItemRepository,
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
        this.examPaperRepository = examPaperRepository;
        this.examPaperQuestionRepository = examPaperQuestionRepository;
        this.attemptRepository = attemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.pythonPaperRepository = pythonPaperRepository;
        this.masteryRepository = masteryRepository;
        this.pathRepository = pathRepository;
        this.pathItemRepository = pathItemRepository;
        this.objectMapper = objectMapper;
    }

    public boolean enabled() {
        return enabled;
    }

    /**
     * 为新账号补齐演示数据。
     *
     * 三类数据各自独立判断，互不牵连：
     * 1. 学习记录（技能进度、增长趋势、课程进度、刷题数）
     * 2. 考试记录（含答题明细，保证成绩页与得分一致）
     * 3. 保存过的练习卷
     * 任何一类已经有真实数据就跳过该类，不会覆盖用户自己的操作。
     */
    @Transactional
    public boolean seedIfEmpty(Long userId) {
        if (!enabled || userId == null) return false;
        boolean created = false;

        if (recordRepository.findByUserIdOrderByOccurredAtDesc(userId).isEmpty()) {
            seedCourses(userId);
            seedProblems(userId);
            created = true;
        }
        if (!attemptRepository.existsByUserId(userId)) {
            if (seedExamRecords(userId)) created = true;
        }
        if (pythonPaperRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).isEmpty()) {
            if (seedPythonPapers(userId)) created = true;
        }
        if (masteryRepository.findByUserIdAndCourseKeyOrderByKnowledgePointKeyAsc(userId, PYTHON_COURSE_KEY).isEmpty()) {
            if (seedKnowledgeMastery(userId)) created = true;
        }
        if (pathRepository.findByUserIdAndCourseKeyAndStatus(userId, PYTHON_COURSE_KEY, "active").isEmpty()) {
            if (seedLearningPath(userId)) created = true;
        }

        if (created) log.info("已为账号 {} 生成「我的练习」演示数据", userId);
        return created;
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

    /** 演示考试记录：挑 2 套已发布试卷，生成"已交卷"作答，并写入逐题明细。 */
    private boolean seedExamRecords(Long userId) {
        List<ExamPaper> papers = examPaperRepository
                .findByPublishedTrueAndStatusOrderByPublishTimeDesc(1, PageRequest.of(0, 2))
                .getContent();
        boolean created = false;
        int index = 0;
        for (ExamPaper paper : papers) {
            List<ExamPaperQuestion> questions = examPaperQuestionRepository
                    .findByPaperIdOrderBySortOrderAscIdAsc(paper.getId());
            if (questions.isEmpty()) continue;
            // 两份成绩做出区分，避免记录千篇一律
            double accuracy = index == 0 ? 0.6 : 0.8;
            seedAttempt(userId, paper, questions, accuracy, index);
            created = true;
            index++;
        }
        return created;
    }

    private void seedAttempt(Long userId, ExamPaper paper, List<ExamPaperQuestion> questions,
                             double accuracy, int index) {
        LocalDateTime submittedAt = LocalDateTime.now().minusDays(7L - index * 3L);
        LocalDateTime startedAt = submittedAt.minusMinutes(Math.max(8L, questions.size() * 2L));
        int durationMinutes = paper.getDurationMinutes() == null ? 30 : paper.getDurationMinutes();

        ExamPaperAttempt attempt = new ExamPaperAttempt();
        attempt.setPaperId(paper.getId());
        attempt.setUserId(userId);
        attempt.setAttemptNo(1);
        attempt.setStatus(ExamPaperAttempt.Status.SUBMITTED);
        attempt.setStartedAt(startedAt);
        attempt.setDeadlineAt(startedAt.plusMinutes(durationMinutes));
        attempt.setSubmittedAt(submittedAt);
        attempt.setQuestionCount(questions.size());
        attempt.setAnsweredCount(questions.size());
        attempt.setObjectiveScore(BigDecimal.ZERO);
        attempt.setObjectiveTotalScore(paper.getTotalScore() == null
                ? BigDecimal.valueOf(100) : paper.getTotalScore());
        attempt.setSelectedQuestionIdsJson(writeJson(
                questions.stream().map(ExamPaperQuestion::getId).toList()));
        attempt.setCreateTime(startedAt);
        ExamPaperAttempt saved = attemptRepository.save(attempt);

        int correctTarget = Math.max(0, (int) Math.round(questions.size() * accuracy));
        BigDecimal score = BigDecimal.ZERO;
        for (int i = 0; i < questions.size(); i++) {
            ExamPaperQuestion question = questions.get(i);
            boolean correct = i < correctTarget;
            if (correct && question.getScore() != null) score = score.add(question.getScore());
            attemptAnswerRepository.save(buildAttemptAnswer(saved.getId(), question, correct));
        }

        // 交卷后回写总分，保证「成绩页分数」与「逐题明细」一致
        saved.setObjectiveScore(score);
        saved.setUpdateTime(submittedAt);
        attemptRepository.save(saved);
    }

    private ExamPaperAttemptAnswer buildAttemptAnswer(Long attemptId, ExamPaperQuestion question, boolean correct) {
        ExamPaperAttemptAnswer answer = new ExamPaperAttemptAnswer();
        answer.setAttemptId(attemptId);
        answer.setPaperQuestionId(question.getId());
        answer.setAnswered(true);
        answer.setCorrect(correct);
        answer.setScore(correct && question.getScore() != null ? question.getScore() : BigDecimal.ZERO);
        answer.setAnswerJson(buildAnswerJson(question, correct));
        return answer;
    }

    /** 按题型构造一份作答：答对就用标准答案，答错则换一个选项。 */
    private String buildAnswerJson(ExamPaperQuestion question, boolean correct) {
        try {
            Map<String, Object> key = objectMapper.readValue(
                    question.getAnswerJson(), new TypeReference<Map<String, Object>>() { });
            if ("true_false".equals(question.getType())) {
                boolean right = Boolean.TRUE.equals(key.get("correct"));
                return writeJson(Map.of("correct", correct == right));
            }
            String rightOption = String.valueOf(key.get("correctOption"));
            if (correct) return writeJson(Map.of("correctOption", rightOption));
            String other = firstOtherOption(question, rightOption);
            return other == null ? "{}" : writeJson(Map.of("correctOption", other));
        } catch (Exception ignored) {
            return "{}";
        }
    }

    /** 答错时挑一个与正确答案不同的选项；没有别的选项就返回 null（当作未作答）。 */
    private String firstOtherOption(ExamPaperQuestion question, String rightOption) {
        try {
            Map<String, Object> body = objectMapper.readValue(
                    question.getBodyJson(), new TypeReference<Map<String, Object>>() { });
            Object options = body.get("options");
            if (options instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> option) {
                        String optionKey = String.valueOf(option.get("key"));
                        if (!optionKey.equals(rightOption)) return optionKey;
                    }
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    /** 演示练习卷：从 Python 题库固定抽题保存两份。 */
    private boolean seedPythonPapers(Long userId) {
        List<PythonProblem> problems = problemRepository.findByEnabledTrueOrderByNumberAsc();
        if (problems.isEmpty()) return false;
        List<PythonProblem> easy = new ArrayList<>();
        for (PythonProblem problem : problems) {
            if ("easy".equalsIgnoreCase(problem.getDifficulty())) easy.add(problem);
        }
        saveDemoPaper(userId, "Python 算法练习卷", "all", List.of(), problems, 10, 5);
        saveDemoPaper(userId, "Python 入门专项练习卷", "easy", List.of(),
                easy.isEmpty() ? problems : easy, 5, 2);
        return true;
    }

    private void saveDemoPaper(Long userId, String title, String difficulty, List<String> tags,
                               List<PythonProblem> pool, int size, int daysAgo) {
        List<Long> ids = new ArrayList<>();
        for (PythonProblem problem : pool) {
            if (ids.size() >= size) break;
            ids.add(problem.getId());
        }
        if (ids.isEmpty()) return;
        PythonPaper paper = new PythonPaper();
        paper.setUserId(userId);
        paper.setTitle(title);
        paper.setDifficulty(difficulty);
        paper.setTagsJson(writeJson(tags));
        paper.setQuestionIdsJson(writeJson(ids));
        paper.setQuestionCount(ids.size());
        paper.setTotalScore(ids.size() * PythonPaper.SCORE_PER_QUESTION);
        paper.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        PythonPaper saved = pythonPaperRepository.save(paper);
        // @PrePersist 会覆盖时间，这里再回写一次，让列表时间看起来自然
        saved.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        pythonPaperRepository.save(saved);
    }

    /** 演示掌握度：让知识图谱呈现"已掌握 / 需巩固 / 学习中 / 待解锁"的真实梯度。 */
    private boolean seedKnowledgeMastery(Long userId) {
        for (MasterySeed seed : MASTERY_SEEDS) {
            LearningKnowledgeMastery mastery = new LearningKnowledgeMastery();
            mastery.setUserId(userId);
            mastery.setCourseKey(PYTHON_COURSE_KEY);
            mastery.setKnowledgePointKey(seed.key());
            mastery.setKnowledgePointName(seed.name());
            mastery.setAppliedAttemptIdsJson("[]");
            mastery.setAttemptCount(seed.attempts());
            mastery.setCorrectCount(seed.correct());
            mastery.setWrongCount(seed.wrong());
            mastery.setScore(BigDecimal.valueOf(seed.score()).setScale(2));
            mastery.setConfidence(BigDecimal.valueOf(seed.attempts() > 0 ? 0.7 : 0).setScale(4));
            mastery.setStatus(seed.status());
            if ("weak".equals(seed.status())) {
                mastery.setNextReviewAt(LocalDateTime.now().plusDays(1));
            }
            masteryRepository.save(mastery);
        }
        return true;
    }

    /** 演示学习路径：一条 active 路径 + 7 个按目录顺序排列的环节。 */
    private boolean seedLearningPath(Long userId) {
        LearningPath path = new LearningPath();
        path.setUserId(userId);
        path.setCourseKey(PYTHON_COURSE_KEY);
        path.setGoal("建立 Python 个性化学习路径");
        path.setVersionNo(1);
        path.setStatus("active");
        path.setProfileDigest("demo-profile");
        path.setMasteryDigest("demo-mastery");
        path.setGeneratedAt(LocalDateTime.now().minusDays(8));
        path.setNextReplanAt(LocalDateTime.now().plusDays(6));
        LearningPath saved = pathRepository.save(path);

        int sequence = 1;
        for (PathItemSeed seed : PATH_ITEM_SEEDS) {
            LearningPathItem item = new LearningPathItem();
            item.setPathId(saved.getId());
            item.setItemKey(seed.key());
            item.setKnowledgePoint(seed.key());
            item.setObjective(seed.objective());
            item.setTargetMastery(BigDecimal.valueOf(80).setScale(2));
            item.setPriority(sequence);
            item.setSequenceNo(sequence);
            item.setResourceKindsJson("[]");
            item.setResourceIdsJson("[]");
            item.setStatus(seed.status());
            item.setDeliveryStatus("completed".equals(seed.status()) ? "delivered" : "pending");
            if ("completed".equals(seed.status())) {
                item.setCompletedAt(LocalDateTime.now().minusDays(7));
                item.setDeliveredAt(LocalDateTime.now().minusDays(7));
            }
            pathItemRepository.save(item);
            sequence++;
        }
        return true;
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (Exception ignored) {
            return "[]";
        }
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

    private record MasterySeed(String key, String name, String status,
                               int score, int attempts, int correct, int wrong) { }

    private record PathItemSeed(String key, String objective, String status) { }
}
