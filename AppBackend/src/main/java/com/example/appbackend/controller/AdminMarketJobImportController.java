package com.example.appbackend.controller;

import com.example.appbackend.dto.JobImportRequest;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.MarketJobImportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/jobs")
public class AdminMarketJobImportController {
    private final MarketJobImportService imports;

    public AdminMarketJobImportController(MarketJobImportService imports) {
        this.imports = imports;
    }

    @PostMapping("/import-batch")
    public Result<MarketJobImportService.ImportResult> importBatch(
            @RequestBody @Valid @Size(max = 200) List<@Valid JobImportRequest> jobs,
            HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "仅管理员可导入岗位数据");
        }
        return Result.success(imports.importBatch(jobs));
    }
}
