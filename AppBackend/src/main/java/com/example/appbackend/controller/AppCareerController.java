package com.example.appbackend.controller;

import com.example.appbackend.dto.CareerFitDTO;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.CareerFitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/app/career")
@Tag(name = "岗位匹配", description = "岗位探索页的人岗匹配与岗位排行")
public class AppCareerController {

    private final CareerFitService careerFitService;

    public AppCareerController(CareerFitService careerFitService) {
        this.careerFitService = careerFitService;
    }

    @GetMapping("/job-fit")
    @Operation(summary = "人岗匹配", description = "按岗位返回匹配度、已掌握、待提升与逐技能差距")
    public Result<CareerFitDTO> jobFit(@RequestParam String jobName, HttpServletRequest request) {
        return Result.success(careerFitService.jobFit(jobName, requireUserId(request)));
    }

    @GetMapping("/fit-jobs")
    @Operation(summary = "岗位匹配排行", description = "返回与当前学生匹配度较高的其它岗位")
    public Result<List<CareerFitDTO.JobFitSummary>> fitJobs(@RequestParam(required = false) String jobName,
                                                            @RequestParam(required = false) Integer limit,
                                                            HttpServletRequest request) {
        return Result.success(careerFitService.fitJobs(jobName, requireUserId(request), limit));
    }

    private Long requireUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (!(raw instanceof Number number)) {
            throw new BusinessException(Result.UNAUTHORIZED_CODE, "请先登录");
        }
        return number.longValue();
    }
}
