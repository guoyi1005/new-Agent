package com.example.appbackend.controller;

import com.example.appbackend.dto.MarketJobDtos;
import com.example.appbackend.entity.Result;
import com.example.appbackend.service.MarketJobQueryService;
import com.example.appbackend.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class MarketJobsController {
    private final MarketJobQueryService service;
    private final JwtUtil jwtUtil;
    public MarketJobsController(MarketJobQueryService service, JwtUtil jwtUtil) { this.service = service; this.jwtUtil = jwtUtil; }

    @GetMapping("/recommendations/internships")
    public Result<MarketJobDtos.Items<MarketJobDtos.InternshipItem>> internships(
            @RequestParam(defaultValue = "4") int limit, HttpServletRequest request) {
        Long userId = userId(request);
        return Result.success(new MarketJobDtos.Items<>(service.internships(userId, limit)));
    }

    @GetMapping("/hot")
    public Result<MarketJobDtos.Items<MarketJobDtos.HotItem>> hot(
            @RequestParam(defaultValue = "6") int limit,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String jobType) {
        return Result.success(new MarketJobDtos.Items<>(service.hot(limit, city, jobType)));
    }

    @GetMapping("/market/{normalizedTitle}")
    public Result<MarketJobDtos.HotItem> market(@PathVariable String normalizedTitle,
                                                 @RequestParam(required = false) String city,
                                                 @RequestParam(required = false) String jobType) {
        return service.market(normalizedTitle, city, jobType).map(Result::success).orElseGet(() -> Result.success(null));
    }

    private Long userId(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        try { return jwtUtil.getUserIdFromToken(authorization.substring(7)); }
        catch (RuntimeException ignored) { return null; }
    }
}
