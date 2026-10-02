package com.example.appbackend.controller;

import com.example.appbackend.dto.LocalJobPostingDTO;
import com.example.appbackend.dto.LocalJobSummaryDTO;
import com.example.appbackend.entity.Result;
import com.example.appbackend.service.LocalJobIngestService;
import com.example.appbackend.service.LocalJobQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成都本地就业岗位：数据来自本地抓取表，每天由调度任务更新。
 * 岗位内容取自招聘网站公开展示的信息，联系方式等个人信息不入库。
 */
@RestController
@RequestMapping("/api/app/local-jobs")
public class AppLocalJobController {

    private final LocalJobQueryService localJobQueryService;
    private final LocalJobIngestService localJobIngestService;

    public AppLocalJobController(
            LocalJobQueryService localJobQueryService,
            LocalJobIngestService localJobIngestService) {
        this.localJobQueryService = localJobQueryService;
        this.localJobIngestService = localJobIngestService;
    }

    @GetMapping("/summary")
    public Result<LocalJobSummaryDTO> summary() {
        return Result.success(localJobQueryService.summary());
    }

    @GetMapping
    public Result<List<LocalJobPostingDTO>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(localJobQueryService.list(page, size, keyword));
    }

    /** 手动触发一次抓取，用于数据为空或临时想刷新时。 */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh() {
        LocalJobIngestService.RefreshResult result = localJobIngestService.refreshAll();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fetched", result.fetched());
        payload.put("created", result.created());
        payload.put("updated", result.updated());
        payload.put("sources", result.sources());
        payload.put("failures", result.failures());
        payload.put("finishedAt", result.finishedAt());
        return Result.success(payload);
    }
}
