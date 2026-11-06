package com.example.appbackend.controller;

import com.example.appbackend.entity.PythonProblem;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.PythonProblemRepository;
import com.example.appbackend.service.CodeJudgeService;
import com.example.appbackend.service.LearningRecordService;
import com.example.appbackend.service.PythonProblemService;
import com.example.appbackend.service.SimpleRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/code")
@Tag(name = "代码执行", description = "Python 在线编程判题接口")
public class CodeExecuteController {

    private static final int MAX_CODE_CHARS = 50_000;

    private final CodeJudgeService codeJudgeService;
    private final PythonProblemService pythonProblemService;
    private final PythonProblemRepository problemRepository;
    private final LearningRecordService learningRecordService;
    private final SimpleRateLimiter rateLimiter;

    public CodeExecuteController(CodeJudgeService codeJudgeService,
                                 PythonProblemService pythonProblemService,
                                 PythonProblemRepository problemRepository,
                                 LearningRecordService learningRecordService,
                                 SimpleRateLimiter rateLimiter) {
        this.codeJudgeService = codeJudgeService;
        this.pythonProblemService = pythonProblemService;
        this.problemRepository = problemRepository;
        this.learningRecordService = learningRecordService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/execute")
    @Operation(summary = "执行 Python 代码并判题", description = "测试用例和函数名只能由服务端题目配置决定")
    public Result<Map<String, Object>> execute(@Valid @RequestBody ExecuteRequest req,
                                                HttpServletRequest request) {
        Long userId = currentUserId(request);
        rateLimiter.check("code-judge:" + userId, 10, Duration.ofMinutes(1),
                "判题请求过于频繁，请稍后重试");
        if (req.code.length() > MAX_CODE_CHARS) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "提交代码过长");
        }

        CodeJudgeService.JudgeSpec spec = pythonProblemService.getJudgeSpec(req.problemId);
        Map<String, Object> result = codeJudgeService.judge(req.code, spec.funcName(), spec.testcases());
        if ("ac".equals(result.get("status"))) {
            PythonProblem problem = problemRepository.findById(req.problemId)
                    .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "题目不存在"));
            int recorded = learningRecordService.recordProblemSolved(userId, problem).size();
            result.put("recorded", recorded);
        }
        return Result.success(result);
    }

    private Long currentUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (!(raw instanceof Number number)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录后进行在线判题");
        }
        return number.longValue();
    }

    public static class ExecuteRequest {
        @NotNull(message = "题目ID不能为空")
        public Long problemId;

        @NotBlank(message = "代码不能为空")
        @Size(max = MAX_CODE_CHARS, message = "提交代码过长")
        public String code;
    }
}
