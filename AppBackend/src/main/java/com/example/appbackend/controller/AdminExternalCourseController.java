package com.example.appbackend.controller;

import com.example.appbackend.dto.AdminExternalCourseDTO;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.LearningSkillRepository;
import com.example.appbackend.service.ExternalCourseService;
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
 * 管理端外部精选资源：对应学生端「课程与专项」里的「外部精选」。
 * 只维护标题、来源、链接与技能标签，不提供任何第三方内容抓取。
 */
@RestController
@RequestMapping("/api/admin/external-courses")
public class AdminExternalCourseController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final ExternalCourseService externalCourseService;
    private final LearningSkillRepository skillRepository;

    public AdminExternalCourseController(ExternalCourseService externalCourseService,
                                         LearningSkillRepository skillRepository) {
        this.externalCourseService = externalCourseService;
        this.skillRepository = skillRepository;
    }

    @GetMapping
    public Result<List<AdminExternalCourseDTO.View>> list(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(externalCourseService.listAll());
    }

    @GetMapping("/skills")
    public Result<List<AdminExternalCourseDTO.SkillOption>> skills(HttpServletRequest request) {
        requireAdmin(request);
        List<AdminExternalCourseDTO.SkillOption> options = skillRepository
                .findAllByStatusOrderBySortOrderAscIdAsc("ACTIVE")
                .stream()
                .map(skill -> new AdminExternalCourseDTO.SkillOption(
                        skill.getId(), skill.getName(), skill.getCategory()))
                .toList();
        return Result.success(options);
    }

    @PostMapping
    public Result<AdminExternalCourseDTO.View> create(@Valid @RequestBody AdminExternalCourseDTO.Request body,
                                                      HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("创建成功", externalCourseService.create(body));
    }

    @PutMapping("/{id}")
    public Result<AdminExternalCourseDTO.View> update(@PathVariable Long id,
                                                      @Valid @RequestBody AdminExternalCourseDTO.Request body,
                                                      HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("保存成功", externalCourseService.update(id, body));
    }

    @PostMapping("/{id}/status")
    public Result<AdminExternalCourseDTO.View> changeStatus(@PathVariable Long id,
                                                            @RequestParam String status,
                                                            HttpServletRequest request) {
        requireAdmin(request);
        return Result.success("状态已更新", externalCourseService.changeStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        externalCourseService.delete(id);
        return Result.success();
    }

    private void requireAdmin(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        if (!ROLE_ADMIN.equals(request.getAttribute("role"))) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "仅管理员可管理外部精选课程");
        }
    }
}
