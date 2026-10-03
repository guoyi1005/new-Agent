package com.example.appbackend.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JobImportRequest(
        @NotBlank @Size(max = 40) String source,
        @Size(max = 160) String sourceJobId,
        @NotBlank @Size(max = 1200) String sourceUrl,
        @NotBlank @Size(max = 240) String jobTitle,
        @NotBlank @Size(max = 100) String normalizedTitle,
        @NotBlank @Size(max = 240) String companyName,
        @Size(max = 100) String city,
        @Size(max = 100) String district,
        @Size(max = 120) String salaryText,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        @Size(max = 24) String salaryUnit,
        Integer salaryMonths,
        @Size(max = 60) String education,
        @Size(max = 80) String experience,
        @NotBlank @Size(max = 40) String jobType,
        String description,
        String requirements,
        @Size(max = 160) String companyIndustry,
        @Size(max = 80) String companySize,
        String publishTime,
        String crawlTime,
        String lastSeenTime,
        @NotBlank @Size(min = 64, max = 64) String fingerprint,
        @Size(max = 64) String contentHash,
        @Min(0) @Max(100) Integer qualityScore,
        @NotNull Boolean isActive,
        @NotNull Boolean isRecent,
        List<@Size(max = 80) String> skills
) {}
