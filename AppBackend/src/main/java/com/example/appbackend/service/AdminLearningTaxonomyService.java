package com.example.appbackend.service;

import com.example.appbackend.dto.AdminLearningTaxonomyDTO.JobOption;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.ProjectRequest;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.ProjectView;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.RequirementRequest;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.RequirementView;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.SkillOption;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.SkillRequest;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.SkillView;
import com.example.appbackend.entity.JobSkillRequirement;
import com.example.appbackend.entity.LearningContentSkill;
import com.example.appbackend.entity.LearningProject;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.JobSkillRequirementRepository;
import com.example.appbackend.repository.LearningContentSkillRepository;
import com.example.appbackend.repository.LearningProjectRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理端学习内容配置服务。
 *
 * <p>技能字典、岗位技能要求、岗位实战任务是学生端「推荐学习」的输入：
 * 本服务只维护这三张表，不会自动修改学生的学习记录。</p>
 */
@Service
public class AdminLearningTaxonomyService {

    private static final String SOURCE_PROJECT = "PROJECT";
    private static final String SKILL_ACTIVE = "ACTIVE";
    private static final String SKILL_INACTIVE = "INACTIVE";

    private final LearningSkillRepository skillRepository;
    private final JobSkillRequirementRepository requirementRepository;
    private final LearningContentSkillRepository contentSkillRepository;
    private final LearningProjectRepository projectRepository;

    public AdminLearningTaxonomyService(LearningSkillRepository skillRepository,
                                        JobSkillRequirementRepository requirementRepository,
                                        LearningContentSkillRepository contentSkillRepository,
                                        LearningProjectRepository projectRepository) {
        this.skillRepository = skillRepository;
        this.requirementRepository = requirementRepository;
        this.contentSkillRepository = contentSkillRepository;
        this.projectRepository = projectRepository;
    }

    // ---------------- 技能字典 ----------------

    @Transactional(readOnly = true)
    public List<SkillView> listSkills() {
        List<SkillView> result = new ArrayList<>();
        for (LearningSkill skill : skillRepository.findAll()) {
            SkillView view = new SkillView();
            view.setId(skill.getId());
            view.setCode(skill.getCode());
            view.setName(skill.getName());
            view.setCategory(skill.getCategory());
            view.setDescription(skill.getDescription());
            view.setSortOrder(skill.getSortOrder());
            view.setStatus(skill.getStatus());
            view.setJobRequirementCount(requirementRepository.findBySkillId(skill.getId()).size());
            view.setContentUsageCount(contentSkillRepository.findBySkillId(skill.getId()).size());
            result.add(view);
        }
        result.sort((left, right) -> {
            int byOrder = Integer.compare(
                    left.getSortOrder() == null ? 0 : left.getSortOrder(),
                    right.getSortOrder() == null ? 0 : right.getSortOrder());
            if (byOrder != 0) return byOrder;
            return Long.compare(left.getId() == null ? 0L : left.getId(),
                    right.getId() == null ? 0L : right.getId());
        });
        return result;
    }

    @Transactional(readOnly = true)
    public List<SkillOption> listSkillOptions() {
        List<SkillOption> options = new ArrayList<>();
        for (LearningSkill skill : skillRepository.findAllByStatusOrderBySortOrderAscIdAsc(SKILL_ACTIVE)) {
            SkillOption option = new SkillOption();
            option.setId(skill.getId());
            option.setCode(skill.getCode());
            option.setName(skill.getName());
            option.setCategory(skill.getCategory());
            options.add(option);
        }
        return options;
    }

    @Transactional
    public SkillView createSkill(SkillRequest request) {
        String code = request.getCode() == null ? "" : request.getCode().trim();
        if (skillRepository.findByCode(code).isPresent()) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "技能编码已存在：" + code);
        }
        LearningSkill skill = new LearningSkill();
        applySkill(skill, request);
        return toSkillView(skillRepository.save(skill));
    }

    @Transactional
    public SkillView updateSkill(Long id, SkillRequest request) {
        LearningSkill skill = requireSkill(id);
        String code = request.getCode() == null ? "" : request.getCode().trim();
        skillRepository.findByCode(code)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessException(Result.BAD_REQUEST_CODE, "技能编码已被占用：" + code);
                });
        applySkill(skill, request);
        return toSkillView(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(Long id) {
        LearningSkill skill = requireSkill(id);
        if (!requirementRepository.findBySkillId(id).isEmpty()) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "该技能仍被岗位技能要求引用，请先移除对应要求");
        }
        if (!contentSkillRepository.findBySkillId(id).isEmpty()) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "该技能仍被课程/题目/项目引用，请先解除关联");
        }
        skillRepository.delete(skill);
    }

    private void applySkill(LearningSkill skill, SkillRequest request) {
        skill.setCode(request.getCode() == null ? null : request.getCode().trim());
        skill.setName(request.getName() == null ? null : request.getName().trim());
        skill.setCategory(request.getCategory());
        skill.setDescription(request.getDescription());
        skill.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        skill.setStatus(SKILL_INACTIVE.equalsIgnoreCase(request.getStatus()) ? SKILL_INACTIVE : SKILL_ACTIVE);
    }

    private SkillView toSkillView(LearningSkill skill) {
        SkillView view = new SkillView();
        view.setId(skill.getId());
        view.setCode(skill.getCode());
        view.setName(skill.getName());
        view.setCategory(skill.getCategory());
        view.setDescription(skill.getDescription());
        view.setSortOrder(skill.getSortOrder());
        view.setStatus(skill.getStatus());
        view.setJobRequirementCount(requirementRepository.findBySkillId(skill.getId()).size());
        view.setContentUsageCount(contentSkillRepository.findBySkillId(skill.getId()).size());
        return view;
    }

    // ---------------- 岗位技能要求 ----------------

    @Transactional(readOnly = true)
    public List<JobOption> listJobs() {
        Map<String, JobOption> byCode = new LinkedHashMap<>();
        for (JobSkillRequirement requirement : requirementRepository.findAllByOrderByJobCodeAscSortOrderAscIdAsc()) {
            if (requirement.getJobCode() == null) continue;
            JobOption option = byCode.computeIfAbsent(requirement.getJobCode(), code -> {
                JobOption created = new JobOption();
                created.setCode(code);
                created.setName(requirement.getJobName());
                created.setSkillCount(0L);
                return created;
            });
            option.setSkillCount(option.getSkillCount() + 1);
        }
        return new ArrayList<>(byCode.values());
    }

    @Transactional(readOnly = true)
    public List<RequirementView> listRequirements(String jobCode) {
        List<JobSkillRequirement> rows = (jobCode == null || jobCode.isBlank())
                ? requirementRepository.findAllByOrderByJobCodeAscSortOrderAscIdAsc()
                : requirementRepository.findByJobCodeOrderBySortOrderAscIdAsc(jobCode);
        Map<Long, LearningSkill> skillById = skillsById();
        List<RequirementView> result = new ArrayList<>();
        for (JobSkillRequirement row : rows) {
            result.add(toRequirementView(row, skillById.get(row.getSkillId())));
        }
        return result;
    }

    @Transactional
    public RequirementView createRequirement(RequirementRequest request) {
        LearningSkill skill = requireSkill(request.getSkillId());
        JobSkillRequirement row = new JobSkillRequirement();
        applyRequirement(row, request);
        return toRequirementView(requirementRepository.save(row), skill);
    }

    @Transactional
    public RequirementView updateRequirement(Long id, RequirementRequest request) {
        JobSkillRequirement row = requirementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "岗位技能要求不存在"));
        LearningSkill skill = requireSkill(request.getSkillId());
        applyRequirement(row, request);
        return toRequirementView(requirementRepository.save(row), skill);
    }

    @Transactional
    public void deleteRequirement(Long id) {
        if (!requirementRepository.existsById(id)) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "岗位技能要求不存在");
        }
        requirementRepository.deleteById(id);
    }

    private void applyRequirement(JobSkillRequirement row, RequirementRequest request) {
        row.setJobCode(request.getJobCode() == null ? null : request.getJobCode().trim());
        row.setJobName(request.getJobName() == null ? null : request.getJobName().trim());
        row.setSkillId(request.getSkillId());
        row.setRequiredLevel(request.getRequiredLevel() == null ? 60 : request.getRequiredLevel());
        row.setImportance(request.getImportance() == null ? 0.5 : request.getImportance());
        row.setRequiredFlag(request.getRequiredFlag() == null || request.getRequiredFlag());
        row.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private RequirementView toRequirementView(JobSkillRequirement row, LearningSkill skill) {
        RequirementView view = new RequirementView();
        view.setId(row.getId());
        view.setJobCode(row.getJobCode());
        view.setJobName(row.getJobName());
        view.setSkillId(row.getSkillId());
        view.setRequiredLevel(row.getRequiredLevel());
        view.setImportance(row.getImportance());
        view.setRequiredFlag(row.getRequiredFlag());
        view.setSortOrder(row.getSortOrder());
        if (skill != null) {
            view.setSkillCode(skill.getCode());
            view.setSkillName(skill.getName());
        }
        return view;
    }

    // ---------------- 岗位实战任务 ----------------

    @Transactional(readOnly = true)
    public List<ProjectView> listProjects() {
        Map<Long, LearningSkill> skillById = skillsById();
        Map<Long, List<String>> jobsBySkillId = jobsBySkillId();
        List<ProjectView> result = new ArrayList<>();
        for (LearningProject project : projectRepository.findAllByOrderBySortOrderAscIdAsc()) {
            ProjectView view = new ProjectView();
            view.setId(project.getId());
            view.setCode(project.getCode());
            view.setTitle(project.getTitle());
            view.setSummary(project.getSummary());
            view.setObjective(project.getObjective());
            view.setDeliverable(project.getDeliverable());
            view.setDifficulty(project.getDifficulty());
            view.setEstimatedHours(project.getEstimatedHours());
            view.setSortOrder(project.getSortOrder());
            view.setStatus(project.getStatus());
            fillProjectSkills(view, skillById, jobsBySkillId);
            result.add(view);
        }
        return result;
    }

    @Transactional
    public ProjectView createProject(ProjectRequest request) {
        String code = request.getCode() == null ? "" : request.getCode().trim();
        if (projectRepository.existsByCode(code)) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "任务编码已存在：" + code);
        }
        LearningProject project = new LearningProject();
        applyProject(project, request);
        LearningProject saved = projectRepository.save(project);
        syncProjectSkills(saved.getId(), request.getSkillIds());
        return requireProjectView(saved.getId());
    }

    @Transactional
    public ProjectView updateProject(Long id, ProjectRequest request) {
        LearningProject project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "岗位实战任务不存在"));
        String code = request.getCode() == null ? "" : request.getCode().trim();
        if (!project.getCode().equals(code) && projectRepository.existsByCode(code)) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "任务编码已存在：" + code);
        }
        applyProject(project, request);
        LearningProject saved = projectRepository.save(project);
        syncProjectSkills(saved.getId(), request.getSkillIds());
        return requireProjectView(saved.getId());
    }

    @Transactional
    public ProjectView changeProjectStatus(Long id, String status) {
        LearningProject project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "岗位实战任务不存在"));
        project.setStatus(LearningProject.STATUS_OFFLINE.equalsIgnoreCase(status)
                ? LearningProject.STATUS_OFFLINE
                : LearningProject.STATUS_ACTIVE);
        projectRepository.save(project);
        return requireProjectView(id);
    }

    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "岗位实战任务不存在");
        }
        contentSkillRepository.deleteBySourceTypeAndSourceId(SOURCE_PROJECT, id);
        projectRepository.deleteById(id);
    }

    private void applyProject(LearningProject project, ProjectRequest request) {
        project.setCode(request.getCode() == null ? null : request.getCode().trim());
        project.setTitle(request.getTitle() == null ? null : request.getTitle().trim());
        project.setSummary(request.getSummary());
        project.setObjective(request.getObjective());
        project.setDeliverable(request.getDeliverable());
        project.setDifficulty(request.getDifficulty());
        project.setEstimatedHours(request.getEstimatedHours());
        project.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        project.setStatus(LearningProject.STATUS_OFFLINE.equalsIgnoreCase(request.getStatus())
                ? LearningProject.STATUS_OFFLINE
                : LearningProject.STATUS_ACTIVE);
    }

    private void syncProjectSkills(Long projectId, List<Long> skillIds) {
        contentSkillRepository.deleteBySourceTypeAndSourceId(SOURCE_PROJECT, projectId);
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
            link.setSourceType(SOURCE_PROJECT);
            link.setSourceId(projectId);
            link.setSkillId(skillId);
            link.setRecommendationOrder(order++);
            contentSkillRepository.save(link);
        }
    }

    private ProjectView requireProjectView(Long id) {
        LearningProject project = projectRepository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "岗位实战任务不存在"));
        Map<Long, LearningSkill> skillById = skillsById();
        Map<Long, List<String>> jobsBySkillId = jobsBySkillId();
        ProjectView view = new ProjectView();
        view.setId(project.getId());
        view.setCode(project.getCode());
        view.setTitle(project.getTitle());
        view.setSummary(project.getSummary());
        view.setObjective(project.getObjective());
        view.setDeliverable(project.getDeliverable());
        view.setDifficulty(project.getDifficulty());
        view.setEstimatedHours(project.getEstimatedHours());
        view.setSortOrder(project.getSortOrder());
        view.setStatus(project.getStatus());
        fillProjectSkills(view, skillById, jobsBySkillId);
        return view;
    }

    private void fillProjectSkills(ProjectView view, Map<Long, LearningSkill> skillById,
                                   Map<Long, List<String>> jobsBySkillId) {
        Set<Long> skillIds = new LinkedHashSet<>();
        Set<String> jobNames = new LinkedHashSet<>();
        for (LearningContentSkill link : contentSkillRepository
                .findBySourceTypeAndSourceId(SOURCE_PROJECT, view.getId())) {
            LearningSkill skill = link.getSkillId() == null ? null : skillById.get(link.getSkillId());
            if (skill == null) continue;
            view.getSkillIds().add(skill.getId());
            view.getSkills().add(skill.getName());
            skillIds.add(skill.getId());
        }
        for (Long skillId : skillIds) {
            jobNames.addAll(jobsBySkillId.getOrDefault(skillId, List.of()));
        }
        view.getJobs().addAll(jobNames);
    }

    // ---------------- 公共 ----------------

    private LearningSkill requireSkill(Long id) {
        if (id == null) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "请选择技能");
        }
        return skillRepository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "技能不存在"));
    }

    private Map<Long, LearningSkill> skillsById() {
        Map<Long, LearningSkill> map = new HashMap<>();
        for (LearningSkill skill : skillRepository.findAll()) {
            map.put(skill.getId(), skill);
        }
        return map;
    }

    private Map<Long, List<String>> jobsBySkillId() {
        Map<Long, List<String>> map = new HashMap<>();
        for (JobSkillRequirement requirement : requirementRepository.findAllByOrderByJobCodeAscSortOrderAscIdAsc()) {
            if (requirement.getSkillId() == null || requirement.getJobName() == null) continue;
            List<String> names = map.computeIfAbsent(requirement.getSkillId(), key -> new ArrayList<>());
            if (!names.contains(requirement.getJobName())) {
                names.add(requirement.getJobName());
            }
        }
        return map;
    }
}
