package com.example.appbackend.service;

import com.example.appbackend.dto.JobImportRequest;
import com.example.appbackend.entity.MarketJob;
import com.example.appbackend.entity.MarketJobSkill;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.MarketJobRepository;
import com.example.appbackend.repository.MarketJobSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Service
public class MarketJobImportService {
    private final MarketJobRepository jobs;
    private final MarketJobSkillRepository skills;
    private final JobTitleNormalizer titleNormalizer;
    private final JobMarketSnapshotService snapshots;

    public MarketJobImportService(MarketJobRepository jobs, MarketJobSkillRepository skills,
                                  JobTitleNormalizer titleNormalizer, JobMarketSnapshotService snapshots) {
        this.jobs = jobs;
        this.skills = skills;
        this.titleNormalizer = titleNormalizer;
        this.snapshots = snapshots;
    }

    @Transactional
    public ImportResult importBatch(List<JobImportRequest> requests) {
        int inserted = 0, updated = 0, skipped = 0;
        for (JobImportRequest request : requests) {
            if (request.qualityScore() == null || request.qualityScore() < 50) {
                skipped++;
                continue;
            }
            if (!request.isActive() || !request.isRecent()) {
                skipped++;
                continue;
            }
            validateSourceUrl(request.sourceUrl());
            MarketJob job = (request.sourceJobId() == null ? java.util.Optional.<MarketJob>empty()
                    : jobs.findBySourceAndSourceJobId(request.source(), request.sourceJobId()))
                    .or(() -> jobs.findByFingerprint(request.fingerprint())).orElseGet(MarketJob::new);
            boolean existed = job.getId() != null;
            boolean unchangedContent = existed && request.contentHash() != null
                    && Objects.equals(job.getContentHash(), request.contentHash());
            apply(job, request);
            job = jobs.save(job);
            if (!unchangedContent) {
                skills.deleteByJobId(job.getId());
                skills.flush();
                for (String skillName : request.skills() == null ? List.<String>of() : request.skills().stream().distinct().toList()) {
                    if (skillName == null || skillName.isBlank()) continue;
                    MarketJobSkill skill = new MarketJobSkill();
                    skill.setJobId(job.getId());
                    skill.setSkillName(skillName.trim());
                    skill.setNormalizedSkill(skillName.trim().toLowerCase(java.util.Locale.ROOT));
                    skill.setSkillCategory("required");
                    skills.save(skill);
                }
            }
            if (existed) updated++; else inserted++;
        }
        if (inserted + updated > 0) snapshots.refreshDaily();
        return new ImportResult(inserted, updated, skipped, inserted + updated);
    }

    private void apply(MarketJob job, JobImportRequest request) {
        LocalDateTime now = LocalDateTime.now();
        job.setSource(request.source());
        job.setSourceJobId(request.sourceJobId());
        job.setSourceUrl(request.sourceUrl());
        job.setJobTitle(request.jobTitle());
        job.setNormalizedTitle(titleNormalizer.normalize(request.normalizedTitle()));
        job.setCompanyName(request.companyName());
        job.setCity(request.city());
        job.setDistrict(request.district());
        job.setJobType(request.jobType());
        job.setSalaryText(request.salaryText());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setSalaryUnit(request.salaryUnit());
        job.setSalaryMonths(request.salaryMonths());
        job.setEducation(request.education());
        job.setExperience(request.experience());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setCompanyIndustry(request.companyIndustry());
        job.setCompanySize(request.companySize());
        job.setPublishTime(parseTime(request.publishTime()));
        job.setCrawlTime(parseTime(request.crawlTime()) == null ? now : parseTime(request.crawlTime()));
        job.setLastSeenTime(parseTime(request.lastSeenTime()) == null ? now : parseTime(request.lastSeenTime()));
        job.setFingerprint(request.fingerprint());
        job.setContentHash(request.contentHash());
        job.setQualityScore(request.qualityScore());
        job.setStatus(Boolean.TRUE.equals(request.isActive()) && Boolean.TRUE.equals(request.isRecent()) ? "ACTIVE" : "INACTIVE");
    }

    private void validateSourceUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null ||
                    !(host.equals("shixiseng.com") || host.endsWith(".shixiseng.com") ||
                      host.equals("nowcoder.com") || host.endsWith(".nowcoder.com"))) {
                throw new IllegalArgumentException("unsupported source URL");
            }
        } catch (RuntimeException exception) {
            throw new BusinessException(400, "岗位来源 URL 无效或不在允许的公开来源中");
        }
    }

    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.replaceAll("\\s*刷新$", "").trim().replace('/', '-');
        try { return OffsetDateTime.parse(normalized).toLocalDateTime(); }
        catch (DateTimeParseException ignored) {
            String timestamp = normalized.length() > 19 ? normalized.substring(0, 19) : normalized;
            try { return LocalDateTime.parse(timestamp); }
            catch (DateTimeParseException ignoredAgain) {
                try { return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
                catch (DateTimeParseException ignoredSpaceFormat) {
                    try { return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-M-d H:m:s")); }
                    catch (DateTimeParseException unsupported) { return null; }
                }
            }
        }
    }

    public record ImportResult(int inserted, int updated, int skipped, int totalSaved) {}
}
