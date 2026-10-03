package com.example.appbackend.controller;

import com.example.appbackend.entity.CareerJobRelation;
import com.example.appbackend.entity.Result;
import com.example.appbackend.service.CareerJobRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端：岗位关系管理（职业路径图谱的连线配置）。 */
@RestController
@RequestMapping("/api/admin/career-job-relations")
@Tag(name = "岗位关系管理", description = "职业路径图谱的岗位关系配置")
public class AdminCareerJobRelationController {

    private final CareerJobRelationService service;

    public AdminCareerJobRelationController(CareerJobRelationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "岗位关系列表")
    public Result<List<CareerJobRelation>> list() {
        return Result.success(service.listAll());
    }

    @PostMapping
    @Operation(summary = "新增岗位关系")
    public Result<CareerJobRelation> create(@RequestBody CareerJobRelation payload) {
        return Result.success("岗位关系已新增", service.create(payload));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改岗位关系")
    public Result<CareerJobRelation> update(@PathVariable Long id, @RequestBody CareerJobRelation payload) {
        return Result.success("岗位关系已保存", service.update(id, payload));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除岗位关系")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success("岗位关系已删除", null);
    }
}
