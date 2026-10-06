package com.example.appbackend.controller;

import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.service.MarketJobRadarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 管理端：岗位雷达（实习岗位抓取）的状态与手动触发。 */
@RestController
@RequestMapping("/api/admin/jobs/radar")
@Tag(name = "岗位雷达", description = "实习岗位的定时抓取与手动刷新")
public class AdminMarketJobRadarController {

    private final MarketJobRadarService radarService;

    public AdminMarketJobRadarController(MarketJobRadarService radarService) {
        this.radarService = radarService;
    }

    @GetMapping("/status")
    @Operation(summary = "岗位雷达状态", description = "返回开关、关键词、在招实习岗位数量与最近一次抓取时间")
    public Result<Map<String, Object>> status(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(radarService.status());
    }

    @PostMapping("/refresh")
    @Operation(summary = "立即抓取一次", description = "可临时指定关键词与每个关键词的条数，用于补数据或小范围验证")
    public Result<MarketJobRadarService.RefreshResult> refresh(
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        requireAdmin(request);
        List<String> keywordList = null;
        if (keywords != null && !keywords.isBlank()) {
            keywordList = new ArrayList<>();
            for (String item : keywords.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    keywordList.add(trimmed);
                }
            }
        }
        return Result.success(radarService.refresh(keywordList, limit));
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getAttribute("role"))) {
            throw new BusinessException(Result.FORBIDDEN_CODE, "仅管理员可触发岗位雷达抓取");
        }
    }
}
