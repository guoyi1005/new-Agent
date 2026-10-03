package com.example.appbackend.service;

import com.example.appbackend.dto.AdminExternalCourseDTO;
import com.example.appbackend.dto.ExternalCourseDTO;
import com.example.appbackend.entity.ExternalCourse;
import com.example.appbackend.entity.JobSkillRequirement;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.Result;
import com.example.appbackend.repository.ExternalCourseRepository;
import com.example.appbackend.repository.JobSkillRequirementRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.exception.BusinessException;
import org.springframework.data.domain.Sort;
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

    // ---------------- 管理端 ----------------

    @Transactional(readOnly = true)
    public List<AdminExternalCourseDTO.View> listAll() {
        return repository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id")))
                .stream()
                .map(this::toAdminView)
                .toList();
    }

    @Transactional
    public AdminExternalCourseDTO.View create(AdminExternalCourseDTO.Request request) {
        ExternalCourse course = new ExternalCourse();
        applyRequest(course, request);
        ExternalCourse saved = repository.save(course);
        syncSkills(saved.getId(), request.getSkillIds());
        return toAdminView(saved);
    }

    @Transactional
    public AdminExternalCourseDTO.View update(Long id, AdminExternalCourseDTO.Request request) {
        ExternalCourse course = repository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "外部课程不存在"));
        applyRequest(course, request);
        ExternalCourse saved = repository.save(course);
        syncSkills(saved.getId(), request.getSkillIds());
        return toAdminView(saved);
    }

    @Transactional
    public AdminExternalCourseDTO.View changeStatus(Long id, String status) {
        ExternalCourse course = repository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "外部课程不存在"));
        course.setStatus(ExternalCourse.STATUS_OFFLINE.equalsIgnoreCase(status)
                ? ExternalCourse.STATUS_OFFLINE
                : ExternalCourse.STATUS_ACTIVE);
        return toAdminView(repository.save(course));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "外部课程不存在");
        }
        contentSkillRepository.deleteBySourceTypeAndSourceId(SOURCE_EXTERNAL_COURSE, id);
        repository.deleteById(id);
    }

    private void applyRequest(ExternalCourse course, AdminExternalCourseDTO.Request request) {
        course.setTitle(request.getTitle() == null ? null : request.getTitle().trim());
        course.setProvider(request.getProvider() == null ? null : request.getProvider().trim());
        course.setUrl(request.getUrl() == null ? null : request.getUrl().trim());
        course.setDescription(request.getDescription());
        course.setLevel(request.getLevel());
        course.setFree(request.getFree() == null || request.getFree());
        course.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        if (request.getStatus() != null && ExternalCourse.STATUS_OFFLINE.equalsIgnoreCase(request.getStatus())) {
            course.setStatus(ExternalCourse.STATUS_OFFLINE);
        } else if (request.getStatus() != null && !request.getStatus().isBlank()) {
            course.setStatus(ExternalCourse.STATUS_ACTIVE);
        }
    }

    private void syncSkills(Long courseId, List<Long> skillIds) {
        contentSkillRepository.deleteBySourceTypeAndSourceId(SOURCE_EXTERNAL_COURSE, courseId);
        contentSkillRepository.flush();
        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }
        int order = 1;
        for (Long skillId : new LinkedHashSet<>(skillIds)) {
            if (skillId == null || !skillRepository.existsById(skillId)) {
                continue;
            }
            LearningContentSkill link = new LearningContentSkill();
            link.setSourceType(SOURCE_EXTERNAL_COURSE);
            link.setSourceId(courseId);
            link.setSkillId(skillId);
            link.setRecommendationOrder(order++);
            contentSkillRepository.save(link);
        }
    }

    private AdminExternalCourseDTO.View toAdminView(ExternalCourse course) {
        AdminExternalCourseDTO.View view = new AdminExternalCourseDTO.View();
        view.setId(course.getId());
        view.setTitle(course.getTitle());
        view.setProvider(course.getProvider());
        view.setUrl(course.getUrl());
        view.setDescription(course.getDescription());
        view.setLevel(course.getLevel());
        view.setFree(course.getFree());
        view.setSortOrder(course.getSortOrder());
        view.setStatus(course.getStatus());
        view.setCreatedAt(course.getCreatedAt());

        Map<Long, LearningSkill> skillById = new HashMap<>();
        for (LearningSkill skill : skillRepository.findAll()) {
            skillById.put(skill.getId(), skill);
        }
        Set<Long> skillIds = new LinkedHashSet<>();
        for (LearningContentSkill link : contentSkillRepository
                .findBySourceTypeAndSourceId(SOURCE_EXTERNAL_COURSE, course.getId())) {
            LearningSkill skill = link.getSkillId() == null ? null : skillById.get(link.getSkillId());
            if (skill == null) continue;
            view.getSkillIds().add(skill.getId());
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
        return view;
    }
}
