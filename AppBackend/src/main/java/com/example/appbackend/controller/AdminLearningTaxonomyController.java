package com.example.appbackend.controller;

import com.example.appbackend.dto.AdminLearningTaxonomyDTO.JobOption;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.ProjectRequest;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.ProjectView;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.RequirementRequest;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.RequirementView;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.SkillOption;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.SkillRequest;
import com.example.appbackend.dto.AdminLearningTaxonomyDTO.SkillView;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.AdminLearningTaxonomyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：学生端「推荐学习」依赖的三张基础表。
 * 这里只维护字典与关联，不会改动学生已有的学习记录。
 */
@RestController
@RequestMapping("/api/admin/learning-taxonomy")
public class AdminLearningTaxonomyController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final AdminLearningTaxonomyService service;

    public AdminLearningTaxonomyController(AdminLearningTaxonomyService service) {
        this.service = service;
    }

    // ---------------- 技能字典 ----------------

    @GetMapping("/skills")
    public Result<List<SkillView>> skills(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(service.listSkills());
    }

    @GetMapping("/skill-options")
    public Result<List<SkillOption>> skillOptions(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(service.listSkillOptions());
    }

    @PostMapping("/skills")
    public Result<SkillView> createSkill(@Valid @RequestBody SkillRequest body, HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("创建成功", service.createSkill(body));
    }

    @PutMapping("/skills/{id}")
    public Result<SkillView> updateSkill(@PathVariable Long id,
                                         @Valid @RequestBody SkillRequest body,
                                         HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("保存成功", service.updateSkill(id, body));
    }

    @DeleteMapping("/skills/{id}")
    public Result<Void> deleteSkill(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        service.deleteSkill(id);
        return Result.success();
    }

    // ---------------- 岗位技能要求 ----------------

    @GetMapping("/jobs")
    public Result<List<JobOption>> jobs(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(service.listJobs());
    }

    @GetMapping("/requirements")
    public Result<List<RequirementView>> requirements(@RequestParam(required = false) String jobCode,
                                                      HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(service.listRequirements(jobCode));
    }

    @PostMapping("/requirements")
    public Result<RequirementView> createRequirement(@Valid @RequestBody RequirementRequest body,
                                                     HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("创建成功", service.createRequirement(body));
    }

    @PutMapping("/requirements/{id}")
    public Result<RequirementView> updateRequirement(@PathVariable Long id,
                                                     @Valid @RequestBody RequirementRequest body,
                                                     HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("保存成功", service.updateRequirement(id, body));
    }

    @DeleteMapping("/requirements/{id}")
    public Result<Void> deleteRequirement(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        service.deleteRequirement(id);
        return Result.success();
    }

    // ---------------- 岗位实战任务 ----------------

    @GetMapping("/projects")
    public Result<List<ProjectView>> projects(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(service.listProjects());
    }

    @PostMapping("/projects")
    public Result<ProjectView> createProject(@Valid @RequestBody ProjectRequest body,
                                             HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("创建成功", service.createProject(body));
    }

    @PutMapping("/projects/{id}")
    public Result<ProjectView> updateProject(@PathVariable Long id,
                                             @Valid @RequestBody ProjectRequest body,
                                             HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("保存成功", service.updateProject(id, body));
    }

    @PostMapping("/projects/{id}/status")
    public Result<ProjectView> changeProjectStatus(@PathVariable Long id,
                                                   @RequestParam String status,
                                                   HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("状态已更新", service.changeProjectStatus(id, status));
    }

    @DeleteMapping("/projects/{id}")
    public Result<Void> deleteProject(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        service.deleteProject(id);
        return Result.success();
    }

    private void requireAdmin(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        if (!ROLE_ADMIN.equals(request.getAttribute("role"))) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "仅管理员可维护学习内容配置");
        }
    }
}
