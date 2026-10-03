package com.example.appbackend.service;

import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseChapter;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningProject;
import com.example.appbackend.entity.LearningRecord;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningRecordRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.util.SkillTextMatcher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一学习记录：课程章节、算法题、学习路径/项目节点完成后都写入同一张 learning_record 表，
 * 供技能状态、推荐学习与“我的练习”聚合使用。
 *
 * 进度口径：记录里保存的是「本次事件之后该技能的累计等级」（0-100），
 * 因此取历史最大值即为当前技能水平。
 */
@Service
public class LearningRecordService {

    public static final String SOURCE_COURSE = "COURSE";
    public static final String SOURCE_COURSE_CHAPTER = "COURSE_CHAPTER";
    public static final String SOURCE_PROBLEM = "PROBLEM";
    public static final String SOURCE_PROJECT = "PROJECT";
    /** 岗位实战任务完成记录：与学习路径节点的 PROJECT 事件分开，避免主键相同时事件冲突。 */
    public static final String SOURCE_PROJECT_TASK = "PROJECT_TASK";
    public static final String SOURCE_SPECIAL_TRAINING = "SPECIAL_TRAINING";
    public static final String SOURCE_PYTHON = "PYTHON";

    public static final String ACTION_COURSE_CHAPTER_COMPLETED = "COURSE_CHAPTER_COMPLETED";
    public static final String ACTION_PROBLEM_SOLVED = "PROBLEM_SOLVED";
    public static final String ACTION_PROJECT_COMPLETED = "PROJECT_COMPLETED";
    public static final String ACTION_PROJECT_TASK_COMPLETED = "PROJECT_TASK_COMPLETED";

    /** 刷题带来的技能增量：简单 10 / 中等 15 / 困难 20。 */
    private static final int PROBLEM_INCREMENT_EASY = 10;
    private static final int PROBLEM_INCREMENT_MEDIUM = 15;
    private static final int PROBLEM_INCREMENT_HARD = 20;
    /** 完成一个学习路径 / 项目节点带来的技能增量。 */
    private static final int PROJECT_INCREMENT = 15;
    /** 完成一个岗位实战任务带来的技能增量。 */
    private static final int PROJECT_TASK_INCREMENT = 25;

    private final LearningRecordRepository recordRepository;
    private final LearningContentSkillRepository contentSkillRepository;
    private final LearningSkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    public LearningRecordService(LearningRecordRepository recordRepository,
                                 LearningContentSkillRepository contentSkillRepository,
                                 LearningSkillRepository skillRepository,
                                 ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.contentSkillRepository = contentSkillRepository;
        this.skillRepository = skillRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 课程章节完成时写回统一学习记录。按 course+chapter+user+skill 生成唯一事件号，重复提交不会产生重复记录。
     * 取消完成状态不删除历史记录，保证学习轨迹可追溯。
     */
    @Transactional
    public List<LearningRecord> recordCourseChapter(Long userId, CampusCourse course, CampusCourseChapter chapter,
                                                    boolean completed, int progressPercent) {
        if (userId == null || course == null || chapter == null || !completed) return List.of();
        // 优先使用章节级技能点；没有配置章节技能时回退到课程级技能。
        List<LearningContentSkill> links =
                contentSkillRepository.findBySourceTypeAndSourceId(SOURCE_COURSE_CHAPTER, chapter.getId());
        if (links.isEmpty()) {
            links = contentSkillRepository.findBySourceTypeAndSourceId(SOURCE_COURSE, course.getId());
        }
        if (links.isEmpty()) return List.of();
        String evidence = "完成课程《" + course.getName() + "》章节《" + chapter.getTitle() + "》";
        String metadata = "{\"courseId\":" + course.getId() + ",\"chapterId\":" + chapter.getId() + "}";
        List<LearningRecord> created = new ArrayList<>();
        for (LearningContentSkill link : links) {
            Long skillId = link.getSkillId();
            if (skillId == null) continue;
            String eventId = courseEventId(course.getId(), chapter.getId(), userId, skillId);
            if (recordRepository.existsByEventId(eventId)) continue;
            created.add(saveRecord(userId, skillId, SOURCE_COURSE, course.getId(),
                    ACTION_COURSE_CHAPTER_COMPLETED, clamp(progressPercent), evidence, metadata, eventId));
        }
        return created;
    }

    /** 算法题通过时按题目标签写回技能进度；同一题同一技能只计一次。 */
    @Transactional
    public List<LearningRecord> recordProblemSolved(Long userId, PythonProblem problem) {
        if (userId == null || problem == null || problem.getId() == null) return List.of();
        List<String> codes = SkillTextMatcher.skillsForProblemTags(readTags(problem.getTags()));
        if (codes.isEmpty()) return List.of();
        String evidence = "通过题目《" + problem.getTitle() + "》";
        String metadata = "{\"problemId\":" + problem.getId() + ",\"difficulty\":"
                + (problem.getDifficulty() == null ? "null" : "\"" + problem.getDifficulty() + "\"") + "}";
        return writeIncremental(userId, SOURCE_PROBLEM, problem.getId(), ACTION_PROBLEM_SOLVED,
                evidence, metadata, codes, problemIncrement(problem.getDifficulty()));
    }

    /** 学习路径 / 项目节点完成时按节点文本写回技能进度；同一节点同一技能只计一次。 */
    @Transactional
    public List<LearningRecord> recordPathItemCompleted(Long userId, Long itemId, String text) {
        if (userId == null || itemId == null) return List.of();
        List<String> codes = SkillTextMatcher.matchText(text);
        if (codes.isEmpty()) return List.of();
        String evidence = "完成学习任务：" + (text == null ? "" : text.trim());
        String metadata = "{\"pathItemId\":" + itemId + "}";
        return writeIncremental(userId, SOURCE_PROJECT, itemId, ACTION_PROJECT_COMPLETED,
                evidence, metadata, codes, PROJECT_INCREMENT);
    }

    /** 岗位实战任务完成时按任务关联的技能写回进度；同一任务同一技能只计一次。 */
    @Transactional
    public List<LearningRecord> recordProjectTaskCompleted(Long userId, LearningProject project) {
        if (userId == null || project == null || project.getId() == null) return List.of();
        List<String> codes = new ArrayList<>();
        for (LearningContentSkill link
                : contentSkillRepository.findBySourceTypeAndSourceId(SOURCE_PROJECT, project.getId())) {
            if (link.getSkillId() == null) continue;
            LearningSkill skill = skillRepository.findById(link.getSkillId()).orElse(null);
            if (skill != null && !codes.contains(skill.getCode())) codes.add(skill.getCode());
        }
        if (codes.isEmpty()) return List.of();
        String evidence = "完成岗位实战任务：" + project.getTitle();
        String metadata = "{\"projectId\":" + project.getId() + ",\"title\":\""
                + (project.getTitle() == null ? "" : project.getTitle().replace("\"", "")) + "\"}";
        return writeIncremental(userId, SOURCE_PROJECT_TASK, project.getId(),
                ACTION_PROJECT_TASK_COMPLETED, evidence, metadata, codes, PROJECT_TASK_INCREMENT);
    }

    /** 已完成岗位实战任务的主键集合，供推荐结果标记「已完成」。 */
    @Transactional(readOnly = true)
    public Set<Long> completedProjectTaskIds(Long userId) {
        Set<Long> ids = new LinkedHashSet<>();
        if (userId == null) return ids;
        for (LearningRecord record : recordRepository.findByUserIdOrderByOccurredAtDesc(userId)) {
            if (SOURCE_PROJECT_TASK.equals(record.getSourceType()) && record.getSourceId() != null) {
                ids.add(record.getSourceId());
            }
        }
        return ids;
    }

    /** 汇总用户在每个技能上的当前等级：取历史记录中的最高进度。 */
    @Transactional(readOnly = true)
    public Map<Long, Integer> currentSkillLevels(Long userId) {
        Map<Long, Integer> levels = new HashMap<>();
        if (userId == null) return levels;
        for (LearningRecord record : recordRepository.findByUserIdOrderByOccurredAtDesc(userId)) {
            if (record.getSkillId() == null) continue;
            int progress = record.getProgress() == null ? 0 : record.getProgress();
            levels.merge(record.getSkillId(), progress, Math::max);
        }
        return levels;
    }

    private List<LearningRecord> writeIncremental(Long userId, String sourceType, Long sourceId, String actionType,
                                                  String evidence, String metadata, List<String> skillCodes,
                                                  int increment) {
        List<LearningRecord> created = new ArrayList<>();
        for (String code : skillCodes) {
            LearningSkill skill = skillRepository.findByCode(code).orElse(null);
            if (skill == null) continue;
            String eventId = ongoingEventId(sourceType, sourceId, userId, skill.getId(), actionType);
            if (recordRepository.existsByEventId(eventId)) continue;
            int next = clamp(currentSkillLevel(userId, skill.getId()) + increment);
            created.add(saveRecord(userId, skill.getId(), sourceType, sourceId,
                    actionType, next, evidence, metadata, eventId));
        }
        return created;
    }

    private int currentSkillLevel(Long userId, Long skillId) {
        int level = 0;
        for (LearningRecord record : recordRepository.findByUserIdAndSkillIdOrderByOccurredAtDesc(userId, skillId)) {
            if (record.getProgress() != null) {
                level = Math.max(level, record.getProgress());
            }
        }
        return level;
    }

    private LearningRecord saveRecord(Long userId, Long skillId, String sourceType, Long sourceId,
                                      String actionType, int progress, String evidence,
                                      String metadata, String eventId) {
        LearningRecord record = new LearningRecord();
        record.setUserId(userId);
        record.setSkillId(skillId);
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setActionType(actionType);
        record.setProgress(progress);
        record.setEvidence(evidence);
        record.setMetadataJson(metadata);
        record.setEventId(eventId);
        return recordRepository.save(record);
    }

    private List<String> readTags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private int problemIncrement(String difficulty) {
        if (difficulty == null) return PROBLEM_INCREMENT_MEDIUM;
        return switch (difficulty.toLowerCase()) {
            case "easy" -> PROBLEM_INCREMENT_EASY;
            case "hard" -> PROBLEM_INCREMENT_HARD;
            default -> PROBLEM_INCREMENT_MEDIUM;
        };
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String courseEventId(Long courseId, Long chapterId, Long userId, Long skillId) {
        return "course-" + courseId + "-chapter-" + chapterId + "-user-" + userId + "-skill-" + skillId + "-completed";
    }

    private String ongoingEventId(String sourceType, Long sourceId, Long userId, Long skillId, String actionType) {
        return sourceType.toLowerCase() + "-" + sourceId + "-user-" + userId + "-skill-" + skillId
                + "-" + actionType.toLowerCase();
    }
}