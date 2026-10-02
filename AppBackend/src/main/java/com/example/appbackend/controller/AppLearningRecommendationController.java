package com.example.appbackend.controller;

import com.example.appbackend.dto.LearningRecommendationDTO;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.LearningRecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/app/learning")
public class AppLearningRecommendationController {

    private final LearningRecommendationService recommendationService;

    public AppLearningRecommendationController(LearningRecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /** 按目标岗位返回统一的技能差距推荐内容；无岗位或无数据时返回空列表，由前端降级展示。 */
    @GetMapping("/recommendations")
    public Result<List<LearningRecommendationDTO>> recommendations(
            @RequestParam(required = false) String jobName,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        return Result.success(recommendationService.recommend(
                jobName, requireUserId(request), limit == null ? 0 : limit));
    }

    private Long requireUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (!(raw instanceof Number number)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        return number.longValue();
    }
}