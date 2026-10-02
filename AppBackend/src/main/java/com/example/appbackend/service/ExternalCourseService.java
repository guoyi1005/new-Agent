package com.example.appbackend.service;

import com.example.appbackend.dto.ExternalCourseDTO;
import com.example.appbackend.entity.ExternalCourse;
import com.example.appbackend.entity.JobSkillRequirement;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.repository.ExternalCourseRepository;
import com.example.appbackend.repository.JobSkillRequirementRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 外部精选课程：读库 + 挂技能/岗位标签。只返回元信息与官方链接，不代理对方内容。
 */
@Service
public class ExternalCourseService {

    public static final String SOURCE_EXTERNAL_COURSE = "EXTERNAL_COURSE";

    private final ExternalCourseRepository repository;
    private final LearningContentSkillRepository contentSkillRepository;
    private final LearningSkillRepository skillRepository;
    private final JobSkillRequirementRepository jobRequirementRepository;

    public ExternalCourseService(
            ExternalCourseRepository repository,
            LearningContentSkillRepository contentSkillRepository,
            LearningSkillRepository skillRepository,
            JobSkillRequirementRepository jobRequirementRepository
    ) {
        this.repository = repository;
        this.contentSkillRepository = contentSkillRepository;
        this.skillRepository = skillRepository;
        this.jobRequirementRepository = jobRequirementRepository;
    }

    @Transactional(readOnly = true)
    public List<ExternalCourseDTO> list() {
        List<ExternalCourse> courses =
                repository.findByStatusOrderBySortOrderAscIdAsc(ExternalCourse.STATUS_ACTIVE);
        if (courses.isEmpty()) return List.of();

        Map<Long, LearningSkill> skillById = new HashMap<>();
        for (LearningSkill skill : skillRepository.findAll()) {
            skillById.put(skill.getId(), skill);
        }
        Map<Long, List<LearningContentSkill>> linksByCourse = new HashMap<>();
        for (LearningContentSkill link : contentSkillRepository.findBySourceType(SOURCE_EXTERNAL_COURSE)) {
            linksByCourse.computeIfAbsent(link.getSourceId(), key -> new ArrayList<>()).add(link);
        }

        List<ExternalCourseDTO> result = new ArrayList<>();
        for (ExternalCourse course : courses) {
            ExternalCourseDTO view = new ExternalCourseDTO();
            view.setId(course.getId());
            view.setTitle(course.getTitle());
            view.setProvider(course.getProvider());
            view.setUrl(course.getUrl());
            view.setDescription(course.getDescription());
            view.setLevel(course.getLevel());
            view.setFree(course.getFree());

            Set<Long> skillIds = new LinkedHashSet<>();
            for (LearningContentSkill link : linksByCourse.getOrDefault(course.getId(), List.of())) {
                LearningSkill skill = link.getSkillId() == null ? null : skillById.get(link.getSkillId());
                if (skill == null) continue;
                view.getSkills().add(skill.getName());
                skillIds.add(skill.getId());
            }
            Set<String> jobNames = new LinkedHashSet<>();
            for (Long skillId : skillIds) {
                for (JobSkillRequirement requirement : jobRequirementRepository.findBySkillId(skillId)) {
                    if (requirement.getJobName() != null) jobNames.add(requirement.getJobName());
                }
            }
            view.getJobs().addAll(jobNames);
            result.add(view);
        }
        return result;
    }
}