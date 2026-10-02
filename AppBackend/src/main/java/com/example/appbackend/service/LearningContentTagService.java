package com.example.appbackend.service;

import com.example.appbackend.dto.ContentTagDTO;
import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.CampusCourseChapter;
import com.example.appbackend.entity.JobSkillRequirement;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningPathItem;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.repository.CampusCourseChapterRepository;
import com.example.appbackend.repository.CampusCourseRepository;
import com.example.appbackend.repository.JobSkillRequirementRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningPathItemRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.repository.PythonProblemRepository;
import com.example.appbackend.util.SkillTextMatcher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内容标签：课程 / 算法题 / 项目节点统一输出「关联技能 + 适合岗位 + 难度」。
 *
 * 课程标签来自已维护的内容技能关联；算法题标签来自题目自身标签；
 * 项目节点为自由文本，使用关键词匹配。取不到数据时不返回标签，不做推测。
 */
@Service
public class LearningContentTagService {

    public static final String SOURCE_COURSE = "COURSE";
    public static final String SOURCE_COURSE_CHAPTER = "COURSE_CHAPTER";
    public static final String SOURCE_PROBLEM = "PROBLEM";
    public static final String SOURCE_PROJECT = "PROJECT";

    private final LearningContentSkillRepository contentSkillRepository;
    private final LearningSkillRepository skillRepository;
    private final JobSkillRequirementRepository jobRequirementRepository;
    private final CampusCourseRepository courseRepository;
    private final CampusCourseChapterRepository chapterRepository;
    private final PythonProblemRepository problemRepository;
    private final LearningPathItemRepository pathItemRepository;
    private final ObjectMapper objectMapper;

    public LearningContentTagService(
            LearningContentSkillRepository contentSkillRepository,
            LearningSkillRepository skillRepository,
            JobSkillRequirementRepository jobRequirementRepository,
            CampusCourseRepository courseRepository,
            CampusCourseChapterRepository chapterRepository,
            PythonProblemRepository problemRepository,
            LearningPathItemRepository pathItemRepository,
            ObjectMapper objectMapper
    ) {
        this.contentSkillRepository = contentSkillRepository;
        this.skillRepository = skillRepository;
        this.jobRequirementRepository = jobRequirementRepository;
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.problemRepository = problemRepository;
        this.pathItemRepository = pathItemRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ContentTagDTO> contentTags(String sourceType, Long sourceId) {
        String type = (sourceType == null || sourceType.isBlank())
                ? SOURCE_COURSE : sourceType.trim().toUpperCase();
        return switch (type) {
            case SOURCE_COURSE -> courseTags(sourceId);
            case SOURCE_COURSE_CHAPTER -> chapterTags(sourceId);
            case SOURCE_PROBLEM -> problemTags(sourceId);
            case SOURCE_PROJECT -> projectTags(sourceId);
            default -> List.of();
        };
    }

    // ---------------- 课程 ----------------

    private List<ContentTagDTO> courseTags(Long courseId) {
        List<LearningContentSkill> links = contentSkillRepository.findBySourceType(SOURCE_COURSE).stream()
                .filter(link -> courseId == null || courseId.equals(link.getSourceId()))
                .toList();
        if (links.isEmpty()) return List.of();

        Map<Long, LearningSkill> skillById = allSkillsById();
        Map<Long, List<LearningContentSkill>> byCourse = new LinkedHashMap<>();
        for (LearningContentSkill link : links) {
            byCourse.computeIfAbsent(link.getSourceId(), key -> new ArrayList<>()).add(link);
        }

        List<ContentTagDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<LearningContentSkill>> entry : byCourse.entrySet()) {
            CampusCourse course = courseRepository.findById(entry.getKey()).orElse(null);
            if (course == null) continue;
            result.add(courseView(course, entry.getValue(), skillById));
        }
        result.sort(Comparator
                .comparing((ContentTagDTO item) ->
                        item.getRecommendationOrder() == null ? Integer.MAX_VALUE : item.getRecommendationOrder())
                .thenComparing(item -> item.getSourceId() == null ? 0L : item.getSourceId()));
        return result;
    }

    private ContentTagDTO courseView(CampusCourse course, List<LearningContentSkill> links,
                                     Map<Long, LearningSkill> skillById) {
        ContentTagDTO view = new ContentTagDTO();
        view.setSourceType(SOURCE_COURSE);
        view.setSourceId(course.getId());
        view.setTitle(course.getName());

        List<LearningSkill> skills = new ArrayList<>();
        Set<Long> prerequisiteIds = new LinkedHashSet<>();
        Map<Long, Boolean> primaryBySkill = new LinkedHashMap<>();
        Map<Long, Integer> targetBySkill = new LinkedHashMap<>();
        int order = Integer.MAX_VALUE;
        for (LearningContentSkill link : links) {
            LearningSkill skill = link.getSkillId() == null ? null : skillById.get(link.getSkillId());
            if (skill != null) {
                skills.add(skill);
                primaryBySkill.put(skill.getId(), Boolean.TRUE.equals(link.getPrimarySkill()));
                targetBySkill.put(skill.getId(), link.getTargetLevel());
                if (Boolean.TRUE.equals(link.getPrimarySkill()) && link.getDifficulty() != null) {
                    view.setDifficulty(link.getDifficulty());
                }
                if (link.getRecommendationOrder() != null) {
                    order = Math.min(order, link.getRecommendationOrder());
                }
            }
            if (link.getPrerequisiteSkillId() != null) {
                prerequisiteIds.add(link.getPrerequisiteSkillId());
            }
        }
        if (view.getDifficulty() == null) {
            for (LearningContentSkill link : links) {
                if (link.getDifficulty() != null) {
                    view.setDifficulty(link.getDifficulty());
                    break;
                }
            }
        }
        view.setRecommendationOrder(order == Integer.MAX_VALUE ? null : order);
        fillSkills(view, skills, primaryBySkill, targetBySkill);
        Set<Long> skillIds = new LinkedHashSet<>();
        skills.forEach(skill -> skillIds.add(skill.getId()));
        for (Long prerequisiteId : prerequisiteIds) {
            LearningSkill skill = skillById.get(prerequisiteId);
            if (skill == null || skillIds.contains(prerequisiteId)) continue;
            view.getPrerequisites().add(skillTag(skill, null, null));
        }
        fillJobs(view, skillIds);
        return view;
    }

    // ---------------- 课程章节 ----------------

    private List<ContentTagDTO> chapterTags(Long chapterId) {
        List<LearningContentSkill> links = contentSkillRepository.findBySourceType(SOURCE_COURSE_CHAPTER).stream()
                .filter(link -> chapterId == null || chapterId.equals(link.getSourceId()))
                .toList();
        if (links.isEmpty()) return List.of();
        Map<Long, LearningSkill> skillById = allSkillsById();
        Map<Long, List<LearningContentSkill>> byChapter = new LinkedHashMap<>();
        for (LearningContentSkill link : links) {
            byChapter.computeIfAbsent(link.getSourceId(), key -> new ArrayList<>()).add(link);
        }
        List<ContentTagDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<LearningContentSkill>> entry : byChapter.entrySet()) {
            CampusCourseChapter chapter = chapterRepository.findById(entry.getKey()).orElse(null);
            if (chapter == null) continue;
            CampusCourse course = courseRepository.findById(chapter.getCourseId()).orElse(null);
            ContentTagDTO view = new ContentTagDTO();
            view.setSourceType(SOURCE_COURSE_CHAPTER);
            view.setSourceId(chapter.getId());
            view.setTitle((course == null ? "" : course.getName() + " · ") + chapter.getTitle());
            List<LearningSkill> skills = new ArrayList<>();
            Map<Long, Boolean> primaryBySkill = new LinkedHashMap<>();
            Set<Long> skillIds = new LinkedHashSet<>();
            for (LearningContentSkill link : entry.getValue()) {
                LearningSkill skill = link.getSkillId() == null ? null : skillById.get(link.getSkillId());
                if (skill == null) continue;
                skills.add(skill);
                primaryBySkill.put(skill.getId(), Boolean.TRUE.equals(link.getPrimarySkill()));
                skillIds.add(skill.getId());
            }
            fillSkills(view, skills, primaryBySkill, Map.of());
            fillJobs(view, skillIds);
            result.add(view);
        }
        result.sort(Comparator.comparing(item -> item.getSourceId() == null ? 0L : item.getSourceId()));
        return result;
    }

    // ---------------- 算法题 ----------------

    private List<ContentTagDTO> problemTags(Long problemId) {
        List<PythonProblem> problems = problemRepository.findByEnabledTrueOrderByNumberAsc().stream()
                .filter(problem -> problemId == null || problemId.equals(problem.getId()))
                .toList();
        List<ContentTagDTO> result = new ArrayList<>();
        Map<String, LearningSkill> skillByCode = allSkillsByCode();
        for (PythonProblem problem : problems) {
            List<String> codes = SkillTextMatcher.skillsForProblemTags(readTags(problem.getTags()));
            if (codes.isEmpty()) continue;
            List<LearningSkill> skills = new ArrayList<>();
            for (String code : codes) {
                LearningSkill skill = skillByCode.get(code);
                if (skill != null) skills.add(skill);
            }
            if (skills.isEmpty()) continue;
            ContentTagDTO view = new ContentTagDTO();
            view.setSourceType(SOURCE_PROBLEM);
            view.setSourceId(problem.getId());
            view.setTitle(problem.getTitle());
            // 难度沿用题库自带的 easy/medium/hard，由前端原有难度展示负责，这里不重复输出。
            fillSkills(view, skills, Map.of(), Map.of());
            Set<Long> skillIds = new LinkedHashSet<>();
            skills.forEach(skill -> skillIds.add(skill.getId()));
            fillJobs(view, skillIds);
            result.add(view);
        }
        result.sort(Comparator.comparing(item -> item.getSourceId() == null ? 0L : item.getSourceId()));
        return result;
    }

    // ---------------- 学习路径 / 项目节点 ----------------

    private List<ContentTagDTO> projectTags(Long itemId) {
        List<LearningPathItem> items = itemId == null
                ? pathItemRepository.findAll()
                : pathItemRepository.findById(itemId).map(List::of).orElse(List.of());
        List<ContentTagDTO> result = new ArrayList<>();
        Map<String, LearningSkill> skillByCode = allSkillsByCode();
        for (LearningPathItem item : items) {
            String text = (item.getKnowledgePoint() == null ? "" : item.getKnowledgePoint())
                    + " " + (item.getObjective() == null ? "" : item.getObjective());
            List<String> codes = SkillTextMatcher.matchText(text);
            if (codes.isEmpty()) continue;
            List<LearningSkill> skills = new ArrayList<>();
            for (String code : codes) {
                LearningSkill skill = skillByCode.get(code);
                if (skill != null) skills.add(skill);
            }
            if (skills.isEmpty()) continue;
            ContentTagDTO view = new ContentTagDTO();
            view.setSourceType(SOURCE_PROJECT);
            view.setSourceId(item.getId());
            view.setTitle(item.getKnowledgePoint());
            fillSkills(view, skills, Map.of(), Map.of());
            Set<Long> skillIds = new LinkedHashSet<>();
            skills.forEach(skill -> skillIds.add(skill.getId()));
            fillJobs(view, skillIds);
            result.add(view);
        }
        result.sort(Comparator.comparing(item -> item.getSourceId() == null ? 0L : item.getSourceId()));
        return result;
    }

    // ---------------- 公共 ----------------

    private void fillSkills(ContentTagDTO view, List<LearningSkill> skills,
                            Map<Long, Boolean> primaryBySkill, Map<Long, Integer> targetBySkill) {
        Set<Long> seen = new LinkedHashSet<>();
        for (LearningSkill skill : skills) {
            if (skill == null || !seen.add(skill.getId())) continue;
            view.getSkills().add(skillTag(skill, primaryBySkill.get(skill.getId()), targetBySkill.get(skill.getId())));
        }
    }

    private ContentTagDTO.SkillTag skillTag(LearningSkill skill, Boolean primary, Integer targetLevel) {
        ContentTagDTO.SkillTag tag = new ContentTagDTO.SkillTag();
        tag.setSkillId(skill.getId());
        tag.setCode(skill.getCode());
        tag.setName(skill.getName());
        tag.setPrimary(primary);
        tag.setTargetLevel(targetLevel);
        return tag;
    }

    private void fillJobs(ContentTagDTO view, Set<Long> skillIds) {
        Map<String, ContentTagDTO.JobTag> jobs = new LinkedHashMap<>();
        for (Long skillId : skillIds) {
            for (JobSkillRequirement requirement : jobRequirementRepository.findBySkillId(skillId)) {
                if (requirement.getJobCode() == null) continue;
                jobs.computeIfAbsent(requirement.getJobCode(), key -> {
                    ContentTagDTO.JobTag tag = new ContentTagDTO.JobTag();
                    tag.setCode(requirement.getJobCode());
                    tag.setName(requirement.getJobName());
                    return tag;
                });
            }
        }
        view.getJobs().addAll(jobs.values());
    }

    private Map<Long, LearningSkill> allSkillsById() {
        Map<Long, LearningSkill> map = new HashMap<>();
        for (LearningSkill skill : skillRepository.findAll()) {
            map.put(skill.getId(), skill);
        }
        return map;
    }

    private Map<String, LearningSkill> allSkillsByCode() {
        Map<String, LearningSkill> map = new HashMap<>();
        for (LearningSkill skill : skillRepository.findAll()) {
            map.put(skill.getCode(), skill);
        }
        return map;
    }

    private List<String> readTags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }
}