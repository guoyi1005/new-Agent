package com.example.appbackend.controller;

import com.example.appbackend.dto.CareerPathDTO;
import com.example.appbackend.entity.Result;
import com.example.appbackend.service.CareerPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 学生端：职业路径图谱（岗位关系 + 迁移分析）。 */
@RestController
@RequestMapping("/api/app/career-path")
@Tag(name = "职业路径图谱", description = "岗位关系、职业路径与我的可转方向")
public class CareerPathController {

    private final CareerPathService service;

    public CareerPathController(CareerPathService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @Operation(summary = "职业路径图谱数据", description = "返回岗位节点、岗位关系与迁移分析，没有学习记录时只返回关系本身")
    public Result<CareerPathDTO> overview(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        Long userId = raw instanceof Number number ? number.longValue() : null;
        return Result.success(service.overview(userId));
    }
}
