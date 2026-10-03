package com.example.appbackend.controller;

import com.example.appbackend.entity.Result;
import com.example.appbackend.service.EmploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/employment")
@Tag(name = "就业服务", description = "学生端实习就业页的校友企业与校园招聘数据")
public class AppEmploymentController {

    private final EmploymentService employmentService;

    public AppEmploymentController(EmploymentService employmentService) {
        this.employmentService = employmentService;
    }

    @GetMapping("/alumni")
    @Operation(summary = "校友企业列表", description = "返回管理端设为展示中的校友企业")
    public Result<?> listAlumni() {
        return Result.success(employmentService.listPublishedAlumni());
    }

    @GetMapping("/campus-recruitments")
    @Operation(summary = "校园招聘列表", description = "返回管理端已发布的宣讲会、双选会与校招岗位")
    public Result<?> listCampusRecruitments() {
        return Result.success(employmentService.listPublishedCampusRecruitments());
    }

    @GetMapping("/campus-recruitment-summary")
    @Operation(summary = "校园招聘汇总", description = "返回招聘季、宣讲会/双选会场次、岗位数与近期日程")
    public Result<?> campusRecruitmentSummary() {
        return Result.success(employmentService.campusRecruitmentSummary());
    }
}
