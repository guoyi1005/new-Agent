package com.example.appbackend.controller;

import com.example.appbackend.dto.ExternalCourseDTO;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.ExternalCourseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/app/learning")
public class AppLearningExternalCourseController {

    private final ExternalCourseService externalCourseService;

    public AppLearningExternalCourseController(ExternalCourseService externalCourseService) {
        this.externalCourseService = externalCourseService;
    }

    /** 外部精选课程列表：只含元信息与官方链接，前端跳转到原站学习。 */
    @GetMapping("/external-courses")
    public Result<List<ExternalCourseDTO>> list(HttpServletRequest request) {
        requireUserId(request);
        return Result.success(externalCourseService.list());
    }

    private Long requireUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (!(raw instanceof Number number)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        return number.longValue();
    }
}