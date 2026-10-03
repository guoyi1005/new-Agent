package com.example.appbackend.service;

import com.example.appbackend.dto.MarketJobDtos;
import com.example.appbackend.entity.JobMarketSnapshot;
import com.example.appbackend.entity.MarketJob;
import com.example.appbackend.entity.MarketJobSkill;
import com.example.appbackend.repository.JobMarketSnapshotRepository;
import com.example.appbackend.repository.MarketJobRepository;
import com.example.appbackend.repository.MarketJobSkillRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobMarketSnapshotService {
    private final MarketJobRepository jobs;
    private final MarketJobSkillRepository skills;
    private final JobMarketSnapshotRepository snapshots;
    private final JobHotScoreService scorer;
    private final ObjectMapper objectMapper;

    public JobMarketSnapshotService(MarketJobRepository jobs, MarketJobSkillRepository skills,
                                    JobMarketSnapshotRepository snapshots, JobHotScoreService scorer,
                                    ObjectMapper objectMapper) {
        this.jobs = jobs; this.skills = skills; this.snapshots = snapshots; this.scorer = scorer; this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "${jobs.snapshot.cron:0 30 2 * * *}")
    @Transactional
    public void refreshDaily() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from30 = now.minusDays(30), from7 = now.minusDays(7);
        List<MarketJob> active = jobs.findByStatusOrderByPublishTimeDesc("ACTIVE");
        if (active.isEmpty()) return;
        Map<Long, List<MarketJobSkill>> skillMap = skills.findByJobIdIn(active.stream().map(MarketJob::getId).toList())
                .stream().collect(Collectors.groupingBy(MarketJobSkill::getJobId));
        Map<GroupKey, List<MarketJob>> groups = new HashMap<>();
        for (MarketJob job : active) {
            String title = job.getNormalizedTitle();
            String city = blankIfNull(job.getCity());
            String jobType = blankIfNull(job.getJobType());
            Set<GroupKey> keys = new HashSet<>(List.of(
                    new GroupKey(title, city, jobType),
                    new GroupKey(title, city, ""),
                    new GroupKey(title, "", jobType),
                    new GroupKey(title, "", "")));
            for (GroupKey key : keys) {
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(job);
            }
        }
        LocalDate date = LocalDate.now();
        groups.forEach((key, group) -> {
            List<MarketJob> recent = group.stream().filter(j -> j.getPublishTime() != null && j.getPublishTime().isAfter(from30)).toList();
            long new7 = recent.stream().filter(j -> j.getPublishTime().isAfter(from7)).count();
            long intern = group.stream().filter(j -> "internship".equalsIgnoreCase(j.getJobType())).count();
            long campus = group.stream().filter(j -> "internship".equalsIgnoreCase(j.getJobType()) || (j.getExperience() != null && (j.getExperience().contains("不限") || j.getExperience().contains("应届")))).count();
            long large = group.stream().filter(j -> companyHas500Plus(j.getCompanySize())).count();
            List<BigDecimal> salary = group.stream().filter(j -> j.getSalaryMin() != null && j.getSalaryMax() != null)
                    .map(this::monthlyMedian).sorted().toList();
            Map<String, Long> skillCounts = group.stream().flatMap(j -> skillMap.getOrDefault(j.getId(), List.of()).stream())
                    .collect(Collectors.groupingBy(MarketJobSkill::getSkillName, Collectors.counting()));
            List<MarketJobDtos.SkillRatio> topSkillItems = skillCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(4)
                    .map(e -> new MarketJobDtos.SkillRatio(e.getKey(), (double) e.getValue() / group.size())).toList();
            String topSkills;
            try { topSkills = objectMapper.writeValueAsString(topSkillItems); }
            catch (JsonProcessingException exception) { throw new IllegalStateException("无法序列化岗位技能快照", exception); }
            double campusRatio = group.isEmpty() ? 0 : (double) campus / group.size();
            double companyRatio = group.isEmpty() ? 0 : (double) large / group.size();
            BigDecimal p50 = salary.size() < 5 ? null : percentile(salary, .50);
            JobMarketSnapshot row = snapshots.findBySnapshotDateAndNormalizedTitleAndCityAndJobType(date, key.title, key.city, key.jobType)
                    .orElseGet(JobMarketSnapshot::new);
            row.setSnapshotDate(date); row.setNormalizedTitle(key.title); row.setCity(key.city); row.setJobType(key.jobType);
            row.setActiveJobCount(group.size()); row.setNew7dCount(new7); row.setNew30dCount(recent.size());
            row.setSalaryP25(salary.size() < 5 ? null : percentile(salary, .25)); row.setSalaryP50(p50);
            row.setSalaryP75(salary.size() < 5 ? null : percentile(salary, .75));
            row.setInternRatio(group.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf((double)intern/group.size()));
            row.setCampusRatio(BigDecimal.valueOf(campusRatio));
            row.setHotScore(scorer.score(group.size(), new7, p50, companyRatio, campusRatio));
            row.setTopSkillsJson(topSkills);
            snapshots.save(row);
        });
    }

    private BigDecimal monthlyMedian(MarketJob job) {
        BigDecimal median = job.getSalaryMin().add(job.getSalaryMax()).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        return switch (Optional.ofNullable(job.getSalaryUnit()).orElse("month").toLowerCase(Locale.ROOT)) {
            case "day" -> median.multiply(BigDecimal.valueOf(21));
            case "year" -> median.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            default -> median;
        };
    }
    private BigDecimal percentile(List<BigDecimal> values, double percentile) {
        return values.get(Math.max(0, Math.min(values.size() - 1, (int)Math.ceil(percentile * values.size()) - 1)));
    }
    private boolean companyHas500Plus(String size) {
        if (size == null) return false;
        String digits = size.replaceAll("[^0-9]", " ").trim();
        if (digits.isEmpty()) return false;
        String first = digits.split("\\s+")[0];
        try { return Integer.parseInt(first) >= 500; } catch (NumberFormatException ignored) { return false; }
    }
    private String blankIfNull(String value) { return value == null ? "" : value; }
    private record GroupKey(String title, String city, String jobType) {}
}
