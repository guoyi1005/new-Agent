package com.example.appbackend.service;

import com.example.appbackend.dto.LearningRecommendationDTO;
import com.example.appbackend.entity.CampusCourse;
import com.example.appbackend.entity.ExternalCourse;
import com.example.appbackend.entity.JobSkillRequirement;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.repository.CampusCourseRepository;
import com.example.appbackend.repository.ExternalCourseRepository;
import com.example.appbackend.repository.JobSkillRequirementRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 岗位 -> 技能差距 -> 学习内容 的统一推荐，覆盖课程与算法题。
 * 题目/项目/专项按同样的结构继续扩展。
 */
@Service
public class LearningRecommendationService {

    private static final String SOURCE_COURSE = "COURSE";
    private static final String SOURCE_PROBLEM = "PROBLEM";
    private static final String SOURCE_EXTERNAL_COURSE = "EXTERNAL_COURSE";
    private static final int DEFAULT_LIMIT = 8;
    /** 同一个技能最多推荐几道算法题，避免题目刷屏。 */
    private static final int MAX_PROBLEMS_PER_SKILL = 2;
    /** 同一个技能最多推荐几门外部课程，避免铺满跳转链接。 */
    private static final int MAX_EXTERNAL_PER_SKILL = 2;
    /** 算法题对技能差距的固定相关度，低于已维护的课程关联权重。 */
    private static final double PROBLEM_RELEVANCE = 0.75;

    private final JobSkillRequirementRepository jobRequirementRepository;
    private final LearningSkillRepository skillRepository;
    private final LearningContentSkillRepository contentSkillRepository;
    private final CampusCourseRepository courseRepository;
    private final ExternalCourseRepository externalCourseRepository;
    private final PythonProblemRepository problemRepository;
    private final LearningRecordService learningRecordService;
    private final ObjectMapper objectMapper;

    public LearningRecommendationService(
            JobSkillRequirementRepository jobRequirementRepository,
            LearningSkillRepository skillRepository,
            LearningContentSkillRepository contentSkillRepository,
            CampusCourseRepository courseRepository,
            ExternalCourseRepository externalCourseRepository,
            PythonProblemRepository problemRepository,
            LearningRecordService learningRecordService,
            ObjectMapper objectMapper
    ) {
        this.jobRequirementRepository = jobRequirementRepository;
        this.skillRepository = skillRepository;
        this.contentSkillRepository = contentSkillRepository;
        this.courseRepository = courseRepository;
        this.externalCourseRepository = externalCourseRepository;
        this.problemRepository = problemRepository;
        this.learningRecordService = learningRecordService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<LearningRecommendationDTO> recommend(String jobName, Long userId, int limit) {
        if (jobName == null || jobName.isBlank()) return List.of();
        List<JobSkillRequirement> requirements = resolveRequirements(jobName.trim());
        if (requirements.isEmpty()) return List.of();
        Map<Long, Integer> levels = learningRecordService.currentSkillLevels(userId);
        Map<PythonProblem, Set<String>> problemSkills = problemSkillIndex();

        List<LearningRecommendationDTO> scored = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JobSkillRequirement requirement : requirements) {
            LearningSkill skill = skillRepository.findById(requirement.getSkillId()).orElse(null);
            if (skill == null) continue;
            int current = levels.getOrDefault(requirement.getSkillId(), 0);
            int gap = Math.max(0, value(requirement.getRequiredLevel()) - current);
            if (gap <= 0) continue;

            int externalCount = 0;
            for (LearningContentSkill link : contentSkillRepository.findBySkillId(requirement.getSkillId())) {
                double relevance = link.getRelevance() == null ? 0.5 : link.getRelevance();
                double multiplier = Boolean.TRUE.equals(link.getPrimarySkill()) ? 1.2 : 1.0;
                if (SOURCE_COURSE.equals(link.getSourceType())) {
                    CampusCourse course = courseRepository.findById(link.getSourceId()).orElse(null);
                    if (course == null || !CampusCourse.STATUS_PUBLISHED.equals(course.getPublishStatus())) continue;
                    String key = SOURCE_COURSE + "-" + link.getSourceId() + "-" + requirement.getSkillId();
                    if (!seen.add(key)) continue;
                    scored.add(view(skill, requirement, SOURCE_COURSE, link.getSourceId(), course.getName(),
                            current, gap, score(requirement, relevance, multiplier, gap)));
                } else if (SOURCE_EXTERNAL_COURSE.equals(link.getSourceType())
                        && externalCount < MAX_EXTERNAL_PER_SKILL) {
                    ExternalCourse external = externalCourseRepository.findById(link.getSourceId()).orElse(null);
                    if (external == null || !ExternalCourse.STATUS_ACTIVE.equals(external.getStatus())) continue;
                    String key = SOURCE_EXTERNAL_COURSE + "-" + link.getSourceId() + "-" + requirement.getSkillId();
                    if (!seen.add(key)) continue;
                    LearningRecommendationDTO item = view(skill, requirement, SOURCE_EXTERNAL_COURSE,
                            link.getSourceId(), external.getTitle(), current, gap,
                            score(requirement, relevance, multiplier, gap));
                    item.setUrl(external.getUrl());
                    scored.add(item);
                    externalCount++;
                }
            }

            int problemCount = 0;
            for (Map.Entry<PythonProblem, Set<String>> entry : problemSkills.entrySet()) {
                if (problemCount >= MAX_PROBLEMS_PER_SKILL) break;
                if (!entry.getValue().contains(skill.getCode())) continue;
                PythonProblem problem = entry.getKey();
                String key = SOURCE_PROBLEM + "-" + problem.getId() + "-" + requirement.getSkillId();
                if (!seen.add(key)) continue;
                scored.add(view(skill, requirement, SOURCE_PROBLEM, problem.getId(), problem.getTitle(),
                        current, gap, score(requirement, PROBLEM_RELEVANCE, 1.0, gap)));
                problemCount++;
            }
        }
        scored.sort(Comparator.comparingDouble(
                (LearningRecommendationDTO item) -> item.getPriority() == null ? 0d : item.getPriority()).reversed());
        int size = limit <= 0 ? DEFAULT_LIMIT : limit;
        return scored.size() <= size ? scored : new ArrayList<>(scored.subList(0, size));
    }

    private Map<PythonProblem, Set<String>> problemSkillIndex() {
        Map<PythonProblem, Set<String>> index = new java.util.LinkedHashMap<>();
        for (PythonProblem problem : problemRepository.findByEnabledTrueOrderByNumberAsc()) {
            Set<String> codes = new java.util.LinkedHashSet<>(SkillTextMatcher.skillsForProblemTags(readTags(problem.getTags())));
            if (!codes.isEmpty()) index.put(problem, codes);
        }
        return index;
    }

    private LearningRecommendationDTO view(LearningSkill skill, JobSkillRequirement requirement,
                                           String sourceType, Long sourceId, String title,
                                           int current, int gap, double priority) {
        LearningRecommendationDTO view = new LearningRecommendationDTO();
        view.setSourceType(sourceType);
        view.setSourceId(sourceId);
        view.setTitle(title);
        view.setSkillCode(skill.getCode());
        view.setSkillName(skill.getName());
        view.setRequiredLevel(requirement.getRequiredLevel());
        view.setCurrentLevel(current);
        view.setGap(gap);
        view.setReason("目标岗位要求 " + skill.getName() + " " + value(requirement.getRequiredLevel())
                + " 分，你当前为 " + current + " 分");
        view.setPriority(priority);
        return view;
    }

    private List<JobSkillRequirement> resolveRequirements(String jobName) {
        List<JobSkillRequirement> byName = jobRequirementRepository.findByJobNameOrderBySortOrderAscIdAsc(jobName);
        if (!byName.isEmpty()) return byName;
        return jobRequirementRepository.findAllByOrderByJobCodeAscSortOrderAscIdAsc().stream()
                .filter(item -> jobName.equalsIgnoreCase(item.getJobCode()))
                .toList();
    }

    private double score(JobSkillRequirement requirement, double relevance, double multiplier, int gap) {
        double importance = requirement.getImportance() == null ? 0.5 : requirement.getImportance();
        return gap * importance * relevance * multiplier;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
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