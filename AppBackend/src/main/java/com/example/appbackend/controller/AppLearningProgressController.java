package com.example.appbackend.controller;

import com.example.appbackend.dto.LearningPracticeSummaryDTO;
import com.example.appbackend.dto.LearningProjectCompletionRequest;
import com.example.appbackend.entity.LearningProject;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.LearningProjectRepository;
import com.example.appbackend.service.LearningPracticeSummaryService;
import com.example.appbackend.service.LearningRecordService;
import com.example.appbackend.service.SimpleRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * 学习进度回写：算法题通过后把技能进度写入统一学习记录。
 */
@RestController
@RequestMapping("/api/app/learning")
public class AppLearningProgressController {

    private final LearningProjectRepository projectRepository;
    private final LearningRecordService learningRecordService;
    private final LearningPracticeSummaryService summaryService;
    private final SimpleRateLimiter rateLimiter;

    public AppLearningProgressController(LearningRecordService learningRecordService,
                                         LearningProjectRepository projectRepository,
                                         LearningPracticeSummaryService summaryService,
                                         SimpleRateLimiter rateLimiter) {
        this.projectRepository = projectRepository;
        this.learningRecordService = learningRecordService;
        this.summaryService = summaryService;
        this.rateLimiter = rateLimiter;
    }

    /** 「我的练习」聚合：课程进度 + 刷题 + 项目 + 技能增长趋势。 */
    @GetMapping("/practice-summary")
    public Result<LearningPracticeSummaryDTO> practiceSummary(
            @RequestParam(defaultValue = "false") boolean verifiedOnly, HttpServletRequest request) {
        return Result.success(summaryService.summary(requireUserId(request), verifiedOnly));
    }

    /** 自报完成岗位实战任务：记录完成说明，但不会直接增加技能等级。 */
    @PostMapping("/project-tasks/{projectId}/completed")
    public Result<Map<String, Object>> projectTaskCompleted(
            @PathVariable Long projectId,
            @Valid @RequestBody LearningProjectCompletionRequest body,
            HttpServletRequest request) {
        Long userId = requireUserId(request);
        rateLimiter.check("learning-project-complete:" + userId, 10, Duration.ofMinutes(10),
                "完成上报过于频繁，请稍后再试");
        LearningProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "实战任务不存在"));
        int recorded = learningRecordService.recordProjectTaskCompleted(
                userId, project, body.getEvidence()).size();
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("projectId", projectId);
        response.put("recorded", recorded);
        return Result.success(response);
    }

    private Long requireUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (!(raw instanceof Number number)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        return number.longValue();
    }
}
