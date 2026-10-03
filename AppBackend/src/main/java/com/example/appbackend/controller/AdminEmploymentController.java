package com.example.appbackend.controller;

import com.example.appbackend.entity.AlumniEnterprise;
import com.example.appbackend.entity.CampusRecruitment;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.EmploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/employment")
@Tag(name = "就业服务管理", description = "校友企业与校园招聘的后台管理接口")
public class AdminEmploymentController {

    private final EmploymentService employmentService;

    public AdminEmploymentController(EmploymentService employmentService) {
        this.employmentService = employmentService;
    }

    // ========== 校友企业 ==========

    @GetMapping("/alumni")
    @Operation(summary = "校友企业列表")
    public Result<?> listAlumni(@RequestParam(required = false) String keyword, HttpServletRequest request) {
        adminOnly(request);
        return Result.success(employmentService.listAlumni(keyword));
    }

    @PostMapping("/alumni")
    @Operation(summary = "新增校友企业")
    public Result<AlumniEnterprise> createAlumni(@RequestBody AlumniEnterprise body, HttpServletRequest request) {
        adminOnly(request);
        return Result.success("校友企业已创建", employmentService.createAlumni(body));
    }

    @PutMapping("/alumni/{id}")
    @Operation(summary = "更新校友企业")
    public Result<AlumniEnterprise> updateAlumni(@PathVariable Long id,
                                                 @RequestBody AlumniEnterprise body,
                                                 HttpServletRequest request) {
        adminOnly(request);
        return Result.success("校友企业已保存", employmentService.updateAlumni(id, body));
    }

    @DeleteMapping("/alumni/{id}")
    @Operation(summary = "删除校友企业")
    public Result<Void> deleteAlumni(@PathVariable Long id, HttpServletRequest request) {
        adminOnly(request);
        employmentService.deleteAlumni(id);
        return Result.success("校友企业已删除", null);
    }

    // ========== 校园招聘 ==========

    @GetMapping("/campus-recruitments")
    @Operation(summary = "校园招聘列表")
    public Result<?> listCampusRecruitments(@RequestParam(required = false) String keyword,
                                            HttpServletRequest request) {
        adminOnly(request);
        return Result.success(employmentService.listCampusRecruitments(keyword));
    }

    @PostMapping("/campus-recruitments")
    @Operation(summary = "新增校园招聘条目")
    public Result<CampusRecruitment> createCampusRecruitment(@RequestBody CampusRecruitment body,
                                                             HttpServletRequest request) {
        adminOnly(request);
        return Result.success("校园招聘条目已创建", employmentService.createCampusRecruitment(body));
    }

    @PutMapping("/campus-recruitments/{id}")
    @Operation(summary = "更新校园招聘条目")
    public Result<CampusRecruitment> updateCampusRecruitment(@PathVariable Long id,
                                                             @RequestBody CampusRecruitment body,
                                                             HttpServletRequest request) {
        adminOnly(request);
        return Result.success("校园招聘条目已保存", employmentService.updateCampusRecruitment(id, body));
    }

    @DeleteMapping("/campus-recruitments/{id}")
    @Operation(summary = "删除校园招聘条目")
    public Result<Void> deleteCampusRecruitment(@PathVariable Long id, HttpServletRequest request) {
        adminOnly(request);
        employmentService.deleteCampusRecruitment(id);
        return Result.success("校园招聘条目已删除", null);
    }

    private void adminOnly(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "仅管理员可管理就业服务内容");
        }
    }
}
