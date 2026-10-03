package com.example.appbackend.controller;

import com.example.appbackend.dto.AdminLearningStatsDTO.Overview;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.AdminLearningStatsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端：学习数据看板（只读）。 */
@RestController
@RequestMapping("/api/admin/learning-stats")
public class AdminLearningStatsController {

    private static final String ROLE_ADMIN = "ADMIN";

    private final AdminLearningStatsService service;

    public AdminLearningStatsController(AdminLearningStatsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public Result<Overview> overview(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        if (!ROLE_ADMIN.equals(request.getAttribute("role"))) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "仅管理员可查看学习数据看板");
        }
        return Result.success(service.overview());
    }
}
