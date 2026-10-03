package com.example.appbackend.dto;

import java.math.BigDecimal;
import java.util.List;

public final class MarketJobDtos {
    private MarketJobDtos() {}
    public record InternshipItem(Long jobId, String title, String company, String city, String salaryText,
                                 Integer matchScore, BigDecimal hotScore, List<String> matchedSkills,
                                 List<String> missingSkills, String source, String sourceUrl,
                                 java.time.LocalDateTime updatedAt) {}
    public record SkillRatio(String name, double ratio) {}
    public record HotItem(String title, BigDecimal hotScore, long activeJobCount, long new7dCount,
                          Double growthRate, BigDecimal salaryP25, BigDecimal salaryP50,
                          BigDecimal salaryP75, List<SkillRatio> topSkills) {}
    public record Items<T>(List<T> items) {}
}
