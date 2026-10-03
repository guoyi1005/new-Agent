package com.example.appbackend.controller;

import com.example.appbackend.dto.LearningPracticeSummaryDTO;
import com.example.appbackend.entity.LearningProject;
import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.LearningProjectRepository;
import com.example.appbackend.repository.PythonProblemRepository;
import com.example.appbackend.service.LearningPracticeSummaryService;
import com.example.appbackend.service.LearningRecordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 学习进度回写：算法题通过后把技能进度写入统一学习记录。
 */
@RestController
@RequestMapping("/api/app/learning")
public class AppLearningProgressController {

    private final PythonProblemRepository problemRepository;
    private final LearningProjectRepository projectRepository;
    private final LearningRecordService learningRecordService;
    private final LearningPracticeSummaryService summaryService;

    public AppLearningProgressController(PythonProblemRepository problemRepository,
                                         LearningRecordService learningRecordService,
                                         LearningProjectRepository projectRepository,
                                         LearningPracticeSummaryService summaryService) {
        this.problemRepository = problemRepository;
        this.projectRepository = projectRepository;
        this.learningRecordService = learningRecordService;
        this.summaryService = summaryService;
    }

    /** 「我的练习」聚合：课程进度 + 刷题 + 项目 + 技能增长趋势。 */
    @GetMapping("/practice-summary")
    public Result<LearningPracticeSummaryDTO> practiceSummary(
            @RequestParam(defaultValue = "false") boolean verifiedOnly, HttpServletRequest request) {
        return Result.success(summaryService.summary(requireUserId(request), verifiedOnly));
    }

    /** 上报题目通过：幂等，同一题重复提交不会重复累计技能进度。 */
    @PostMapping("/problems/{problemId}/solved")
    public Result<Map<String, Object>> problemSolved(@PathVariable Long problemId, HttpServletRequest request) {
        Long userId = requireUserId(request);
        PythonProblem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "题目不存在"));
        int recorded = learningRecordService.recordProblemSolved(userId, problem).size();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("problemId", problemId);
        body.put("recorded", recorded);
        return Result.success(body);
    }

    /** 上报岗位实战任务完成：幂等，同一任务重复提交不会重复累计技能进度。 */
    @PostMapping("/project-tasks/{projectId}/completed")
    public Result<Map<String, Object>> projectTaskCompleted(@PathVariable Long projectId,
                                                           HttpServletRequest request) {
        Long userId = requireUserId(request);
        LearningProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "实战任务不存在"));
        int recorded = learningRecordService.recordProjectTaskCompleted(userId, project).size();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("projectId", projectId);
        body.put("recorded", recorded);
        return Result.success(body);
    }

    private Long requireUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (!(raw instanceof Number number)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        return number.longValue();
    }
}
