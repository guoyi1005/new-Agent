package com.example.appbackend.service;

import com.example.appbackend.dto.MarketJobDtos;
import com.example.appbackend.entity.InterviewUserProfile;
import com.example.appbackend.entity.MarketJob;
import com.example.appbackend.entity.MarketJobSkill;
import com.example.appbackend.repository.InterviewUserProfileRepository;
import com.example.appbackend.repository.MarketJobRepository;
import com.example.appbackend.repository.MarketJobSkillRepository;
import com.example.appbackend.repository.JobMarketSnapshotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MarketJobQueryService {
    private final MarketJobRepository jobs;
    private final MarketJobSkillRepository skills;
    private final InterviewUserProfileRepository profiles;
    private final JobHotScoreService hotScore;
    private final JobTitleNormalizer titleNormalizer;
    private final SkillExtractor skillExtractor;
    private final JobMarketSnapshotRepository snapshots;
    private final ObjectMapper objectMapper;

    public MarketJobQueryService(MarketJobRepository jobs, MarketJobSkillRepository skills,
                                 InterviewUserProfileRepository profiles, JobHotScoreService hotScore,
                                 JobTitleNormalizer titleNormalizer, SkillExtractor skillExtractor,
                                 JobMarketSnapshotRepository snapshots, ObjectMapper objectMapper) {
        this.jobs = jobs; this.skills = skills; this.profiles = profiles;
        this.hotScore = hotScore; this.titleNormalizer = titleNormalizer; this.skillExtractor = skillExtractor;
        this.snapshots = snapshots; this.objectMapper = objectMapper;
    }

    public List<MarketJobDtos.InternshipItem> internships(Long userId, int limit) {
        InterviewUserProfile profile = userId == null ? null : profiles.findByUserId(userId).orElse(null);
        Set<String> userSkills = profile == null ? Set.of() : extractProfileSkills(profile);
        List<MarketJob> candidates = jobs.findByStatusAndJobTypeIgnoreCaseOrderByPublishTimeDesc("ACTIVE", "internship");
        Map<Long, List<MarketJobSkill>> skillMap = skills.findByJobIdIn(candidates.stream().map(MarketJob::getId).toList())
                .stream().collect(Collectors.groupingBy(MarketJobSkill::getJobId));
        Map<String, Long> demand = candidates.stream().collect(Collectors.groupingBy(MarketJob::getNormalizedTitle, Collectors.counting()));
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        return candidates.stream().map(job -> {
                    List<MarketJobSkill> required = skillMap.getOrDefault(job.getId(), List.of());
                    List<String> matched = required.stream().map(MarketJobSkill::getSkillName).filter(s -> userSkills.contains(normalizeSkill(s))).distinct().toList();
                    List<String> missing = userSkills.isEmpty() ? List.of() : required.stream()
                            .map(MarketJobSkill::getSkillName).filter(s -> !userSkills.contains(normalizeSkill(s)))
                            .distinct().toList();
                    double skillMatch = required.isEmpty() ? 0 : (double) matched.size() / required.size() * 100;
                    double careerMatch = careerScore(profile == null ? null : profile.getTargetPosition(), job.getNormalizedTitle());
                    double educationMatch = educationScore(profile == null ? null : profile.getEducation(), job.getEducation());
                    long count30 = demand.getOrDefault(job.getNormalizedTitle(), 0L);
                    BigDecimal heat = hotScore.score(count30, (int) candidates.stream().filter(j -> Objects.equals(j.getNormalizedTitle(), job.getNormalizedTitle()) && j.getPublishTime() != null && j.getPublishTime().isAfter(cutoff)).count(), null, 0, campusFriendly(job) ? 1 : 0);
                    Double weightedScore = 0d;
                    double usedWeight = 0d;
                    boolean hasPersonalSignal = false;
                    if (!userSkills.isEmpty() && !required.isEmpty()) {
                        weightedScore += skillMatch * .35; usedWeight += .35; hasPersonalSignal = true;
                    }
                    if (profile != null && profile.getTargetPosition() != null && !profile.getTargetPosition().isBlank()) {
                        weightedScore += careerMatch * .20; usedWeight += .20; hasPersonalSignal = true;
                    }
                    if (profile != null && profile.getEducation() != null && !profile.getEducation().isBlank()) {
                        weightedScore += educationMatch * .10; usedWeight += .10; hasPersonalSignal = true;
                    }
                    if (hasPersonalSignal) {
                        weightedScore += (campusFriendly(job) ? 100 : 0) * .10 + heat.doubleValue() * .10;
                        usedWeight += .20;
                    }
                    Integer recommendation = usedWeight == 0 ? null
                            : Math.max(0, Math.min(100, (int) Math.round(weightedScore / usedWeight)));
                    return new MarketJobDtos.InternshipItem(job.getId(), job.getJobTitle(), job.getCompanyName(), job.getCity(), salaryText(job),
                            recommendation, heat, matched, missing, job.getSource(), job.getSourceUrl(), job.getCrawlTime());
                }).sorted(Comparator.comparing(MarketJobDtos.InternshipItem::matchScore,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(1, Math.min(limit, 100))).toList();
    }

    public List<MarketJobDtos.HotItem> hot(int limit, String city, String jobType) {
        Optional<LocalDate> latest = snapshots.findFirstByOrderBySnapshotDateDesc().map(s -> s.getSnapshotDate());
        if (latest.isEmpty()) return List.of();
        String cityKey = city == null || city.isBlank() ? "" : city;
        String typeKey = jobType == null || jobType.isBlank() ? "" : jobType;
        List<com.example.appbackend.entity.JobMarketSnapshot> rows =
                snapshots.findBySnapshotDateAndCityIgnoreCaseAndJobTypeIgnoreCaseOrderByHotScoreDesc(latest.get(), cityKey, typeKey);
        return rows.stream().map(row -> new MarketJobDtos.HotItem(row.getNormalizedTitle(), row.getHotScore(),
                        row.getActiveJobCount(), row.getNew7dCount(), null, row.getSalaryP25(), row.getSalaryP50(), row.getSalaryP75(), parseTopSkills(row.getTopSkillsJson())))
                .limit(Math.max(1, Math.min(limit, 20))).toList();
    }

    public Optional<MarketJobDtos.HotItem> market(String normalizedTitle, String city, String jobType) {
        String cityKey = city == null ? "" : city;
        String typeKey = jobType == null ? "" : jobType;
        return snapshots.findFirstByNormalizedTitleAndCityAndJobTypeOrderBySnapshotDateDesc(normalizedTitle, cityKey, typeKey)
                .map(row -> new MarketJobDtos.HotItem(row.getNormalizedTitle(), row.getHotScore(), row.getActiveJobCount(),
                        row.getNew7dCount(), null, row.getSalaryP25(), row.getSalaryP50(), row.getSalaryP75(), parseTopSkills(row.getTopSkillsJson())));
    }

    private List<MarketJobDtos.SkillRatio> parseTopSkills(String value) {
        if (value == null || value.isBlank()) return List.of();
        try { return objectMapper.readValue(value, new TypeReference<>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private Set<String> extractProfileSkills(InterviewUserProfile profile) {
        String source = String.join(" ", nullToEmpty(profile.getSkillTags()), nullToEmpty(profile.getTechStack()));
        return skillExtractor.extract(source).stream().map(SkillExtractor.Skill::normalized).collect(Collectors.toSet());
    }
    private String normalizeSkill(String skill) { return skill == null ? "" : skill.trim().toLowerCase(Locale.ROOT); }
    private String nullToEmpty(String value) { return value == null ? "" : value; }
    private double careerScore(String target, String title) {
        if (target == null || target.isBlank()) return 50;
        String a = titleNormalizer.normalize(target), b = titleNormalizer.normalize(title);
        return a.equalsIgnoreCase(b) || a.contains(b) || b.contains(a) ? 100 : 20;
    }
    private double educationScore(String current, String required) {
        if (required == null || required.isBlank() || required.contains("不限")) return 100;
        if (current == null || current.isBlank()) return 50;
        return current.equalsIgnoreCase(required) ? 100 : 40;
    }
    private boolean campusFriendly(MarketJob job) {
        return "internship".equalsIgnoreCase(job.getJobType()) || (job.getExperience() != null && (job.getExperience().contains("不限") || job.getExperience().contains("应届")));
    }
    private String salaryText(MarketJob job) {
        if (job.getSalaryText() != null && !job.getSalaryText().isBlank()) return job.getSalaryText();
        if (job.getSalaryMin() == null || job.getSalaryMax() == null) return "薪资信息以原职位为准";
        String suffix = switch (job.getSalaryUnit() == null ? "" : job.getSalaryUnit()) {
            case "day" -> "元/天";
            case "year" -> "万/年";
            default -> "K/月";
        };
        BigDecimal divisor = switch (job.getSalaryUnit() == null ? "" : job.getSalaryUnit()) {
            case "day" -> BigDecimal.ONE;
            case "year" -> BigDecimal.valueOf(10000);
            default -> BigDecimal.valueOf(1000);
        };
        return job.getSalaryMin().divide(divisor, 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "-" +
                job.getSalaryMax().divide(divisor, 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + suffix;
    }
    private BigDecimal percentile(List<BigDecimal> values, double p) {
        if (values.isEmpty()) return null;
        int index = (int) Math.ceil(p * values.size()) - 1;
        return values.get(Math.max(0, Math.min(values.size() - 1, index)));
    }
}
