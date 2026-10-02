package com.example.appbackend.controller;

import com.example.appbackend.dto.ContentTagDTO;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.LearningContentTagService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/app/learning")
public class AppLearningContentTagController {

    private final LearningContentTagService contentTagService;

    public AppLearningContentTagController(LearningContentTagService contentTagService) {
        this.contentTagService = contentTagService;
    }

    /**
     * 内容标签：sourceType 取 COURSE / PROBLEM / PROJECT，不传 sourceId 时返回该类型全部内容的标签。
     */
    @GetMapping("/content-tags")
    public Result<List<ContentTagDTO>> contentTags(
            @RequestParam(required = false, defaultValue = "COURSE") String sourceType,
            @RequestParam(required = false) Long sourceId,
            HttpServletRequest request) {
        requireUserId(request);
        return Result.success(contentTagService.contentTags(sourceType, sourceId));
    }

    private Long requireUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (!(raw instanceof Number number)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        return number.longValue();
    }
}