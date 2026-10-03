package com.example.appbackend.service;

import com.example.appbackend.dto.CareerFitDTO;
import com.example.appbackend.entity.JobProfile;
import com.example.appbackend.entity.JobSkillRequirement;
import com.example.appbackend.entity.LearningSkill;
import com.example.appbackend.repository.JobProfileRepository;
import com.example.appbackend.repository.JobSkillRequirementRepository;
import com.example.appbackend.repository.LearningRecordRepository;
import com.example.appbackend.repository.LearningSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 人岗匹配计算。
 *
 * 数据来源全部是已存在的真实数据，不额外维护一份：
 * 1. 岗位要求：job_skill_requirement（技能 + 要求等级 + 重要性）；
 * 2. 岗位信息：job_profile（方向、类型、职责、建议）；
 * 3. 我的水平：learning_record 汇总出的各技能当前等级（做题 / 课程章节 / 项目任务写入）。
 *
 * 匹配度 = Σ(技能达成率 × 重要性) / Σ(重要性) × 100，
 * 达成率 = min(我的等级, 岗位要求) / 岗位要求。
 */
@Service
public class CareerFitService {

    public static final String STATUS_INSUFFICIENT = "insufficient";
    public static final String STATUS_PARTIAL = "partial";
    public static final String STATUS_READY = "ready";

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 10;
    /** 覆盖技能数达到该值即认为证据足够。 */
    private static final int READY_SKILL_THRESHOLD = 3;

    private final JobProfileRepository jobProfileRepository;
    private final JobSkillRequirementRepository jobRequirementRepository;
    private final LearningSkillRepository skillRepository;
    private final LearningRecordRepository learningRecordRepository;
    private final LearningRecordService learningRecordService;

    public CareerFitService(JobProfileRepository jobProfileRepository,
                            JobSkillRequirementRepository jobRequirementRepository,
                            LearningSkillRepository skillRepository,
                            LearningRecordRepository learningRecordRepository,
                            LearningRecordService learningRecordService) {
        this.jobProfileRepository = jobProfileRepository;
        this.jobRequirementRepository = jobRequirementRepository;
        this.skillRepository = skillRepository;
        this.learningRecordRepository = learningRecordRepository;
        this.learningRecordService = learningRecordService;
    }

    /** 单个岗位的人岗匹配：匹配度、已掌握、待提升与逐技能对照。 */
    @Transactional(readOnly = true)
    public CareerFitDTO jobFit(String jobName, Long userId) {
        String keyword = jobName == null ? "" : jobName.trim();
        JobProfile profile = resolveProfile(keyword);

        CareerFitDTO dto = new CareerFitDTO();
        dto.setJobCode(profile != null ? profile.getCode() : null);
        dto.setJobName(profile != null ? profile.getName() : keyword);
        if (profile != null) {
            dto.setDirection(profile.getDirection());
            dto.setType(profile.getType());
            dto.setSummary(profile.getSummary());
            dto.setResponsibilities(splitResponsibilities(profile.getResponsibilities()));
        }

        List<JobSkillRequirement> requirements = resolveRequirements(profile, keyword);
        Map<Long, Integer> levels = learningRecordService.currentSkillLevels(userId);
        List<CareerFitDTO.SkillFit> fits = buildSkillFits(requirements, levels);
        int evidenceCount = countEvidence(userId);

        dto.setTotalSkills(fits.size());
        dto.setCoveredSkills((int) fits.stream().filter((fit) -> fit.getCurrent() > 0).count());
        dto.setEvidenceCount(evidenceCount);
        dto.setMatchRate(matchRate(fits));
        dto.setMastered(fits.stream()
                .filter((fit) -> fit.getGap() == 0)
                .sorted(Comparator.comparing(CareerFitDTO.SkillFit::getImportance).reversed())
                .toList());
        List<CareerFitDTO.SkillFit> gaps = fits.stream()
                .filter((fit) -> fit.getGap() > 0)
                .sorted(gapPriority())
                .toList();
        dto.setGaps(gaps);
        dto.setToImprove(gaps.stream().limit(3).toList());
        dto.setAdvice(resolveAdvice(profile, dto.getToImprove()));

        String dataStatus = resolveDataStatus(evidenceCount, dto.getCoveredSkills(), fits.size());
        dto.setDataStatus(dataStatus);
        dto.setDataStatusText(dataStatusText(dataStatus, evidenceCount));
        dto.setLevelSourceText("技能等级来自做题、课程章节学习与项目任务完成记录，未积累记录前按 0 计算。");
        return dto;
    }

    /** 岗位匹配排行：用于「适合你的岗位」，按匹配度从高到低。 */
    @Transactional(readOnly = true)
    public List<CareerFitDTO.JobFitSummary> fitJobs(String jobName, Long userId, Integer limit) {
        String keyword = jobName == null ? "" : jobName.trim();
        JobProfile current = resolveProfile(keyword);
        int size = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        List<JobProfile> profiles = jobProfileRepository.findByStatusOrderBySortOrderAscIdAsc(JobProfile.STATUS_ACTIVE);
        if (profiles.isEmpty()) {
            profiles = jobProfileRepository.findAllByOrderBySortOrderAscIdAsc();
        }
        Map<Long, Integer> levels = learningRecordService.currentSkillLevels(userId);
        int evidenceCount = countEvidence(userId);

        List<CareerFitDTO.JobFitSummary> result = new ArrayList<>();
        for (JobProfile profile : profiles) {
            if (current != null && current.getCode().equals(profile.getCode())) {
                continue;
            }
            List<JobSkillRequirement> requirements =
                    jobRequirementRepository.findByJobCodeOrderBySortOrderAscIdAsc(profile.getCode());
            if (requirements.isEmpty()) {
                continue;
            }
            List<CareerFitDTO.SkillFit> fits = buildSkillFits(requirements, levels);
            if (fits.isEmpty()) {
                continue;
            }
            CareerFitDTO.JobFitSummary summary = new CareerFitDTO.JobFitSummary();
            summary.setJobCode(profile.getCode());
            summary.setJobName(profile.getName());
            summary.setDirection(profile.getDirection());
            summary.setType(profile.getType());
            summary.setMatchRate(matchRate(fits));
            summary.setMasteredCount((int) fits.stream().filter((fit) -> fit.getGap() == 0).count());

            // 优势取「已积累记录且达成率最高」的技能；待提升取差距最大的技能，
            // 并排除已被选为优势的同一个技能，避免出现「优势是它、要补的也是它」。
            CareerFitDTO.SkillFit strength = fits.stream()
                    .filter((fit) -> fit.getCurrent() > 0)
                    .max(Comparator.comparingDouble(
                            (CareerFitDTO.SkillFit fit) -> fit.getRatio() * fit.getImportance()))
                    .orElse(null);
            summary.setTopStrength(strength == null ? null : strength.getSkillName());

            List<CareerFitDTO.SkillFit> orderedGaps = fits.stream()
                    .filter((fit) -> fit.getGap() > 0)
                    .sorted(gapPriority())
                    .toList();
            summary.setTopGap(orderedGaps.stream()
                    .filter((fit) -> strength == null || !fit.getSkillCode().equals(strength.getSkillCode()))
                    .findFirst()
                    .or(() -> orderedGaps.stream().findFirst())
                    .map(CareerFitDTO.SkillFit::getSkillName)
                    .orElse(null));
            summary.setTopSkills(fits.stream()
                    .sorted(Comparator.comparing(CareerFitDTO.SkillFit::getImportance).reversed())
                    .limit(3)
                    .map(CareerFitDTO.SkillFit::getSkillName)
                    .toList());
            summary.setDataStatus(resolveDataStatus(evidenceCount,
                    (int) fits.stream().filter((fit) -> fit.getCurrent() > 0).count(), fits.size()));
            result.add(summary);
        }

        result.sort(Comparator.comparing(CareerFitDTO.JobFitSummary::getMatchRate).reversed());
        return result.size() > size ? result.subList(0, size) : result;
    }

    private JobProfile resolveProfile(String jobNameOrCode) {
        if (jobNameOrCode.isBlank()) {
            return null;
        }
        return jobProfileRepository.findByCode(jobNameOrCode)
                .or(() -> jobProfileRepository.findByName(jobNameOrCode))
                .orElse(null);
    }

    private List<JobSkillRequirement> resolveRequirements(JobProfile profile, String rawKeyword) {
        if (profile != null) {
            List<JobSkillRequirement> byCode =
                    jobRequirementRepository.findByJobCodeOrderBySortOrderAscIdAsc(profile.getCode());
            if (!byCode.isEmpty()) {
                return byCode;
            }
            List<JobSkillRequirement> byName =
                    jobRequirementRepository.findByJobNameOrderBySortOrderAscIdAsc(profile.getName());
            if (!byName.isEmpty()) {
                return byName;
            }
        }
        if (rawKeyword.isBlank()) {
            return List.of();
        }
        return jobRequirementRepository.findByJobNameOrderBySortOrderAscIdAsc(rawKeyword);
    }

    private List<CareerFitDTO.SkillFit> buildSkillFits(List<JobSkillRequirement> requirements,
                                                       Map<Long, Integer> levels) {
        List<CareerFitDTO.SkillFit> fits = new ArrayList<>();
        for (JobSkillRequirement requirement : requirements) {
            LearningSkill skill = skillRepository.findById(requirement.getSkillId()).orElse(null);
            if (skill == null) {
                continue;
            }
            int required = requirement.getRequiredLevel() == null ? 0 : requirement.getRequiredLevel();
            if (required <= 0) {
                continue;
            }
            int current = clamp(levels.getOrDefault(requirement.getSkillId(), 0));
            CareerFitDTO.SkillFit fit = new CareerFitDTO.SkillFit();
            fit.setSkillCode(skill.getCode());
            fit.setSkillName(skill.getName());
            fit.setCurrent(current);
            fit.setRequired(required);
            fit.setGap(Math.max(0, required - current));
            fit.setImportance(requirement.getImportance() == null ? 0.5 : requirement.getImportance());
            fit.setRatio(Math.min(current, required) / (double) required);
            fits.add(fit);
        }
        return fits;
    }

    private Integer matchRate(List<CareerFitDTO.SkillFit> fits) {
        double weighted = 0;
        double weightSum = 0;
        for (CareerFitDTO.SkillFit fit : fits) {
            weighted += fit.getRatio() * fit.getImportance();
            weightSum += fit.getImportance();
        }
        if (weightSum <= 0) {
            return 0;
        }
        return (int) Math.round(weighted / weightSum * 100);
    }

    private Comparator<CareerFitDTO.SkillFit> gapPriority() {
        return Comparator.comparingDouble(
                (CareerFitDTO.SkillFit fit) -> fit.getGap() * fit.getImportance()).reversed();
    }

    private int countEvidence(Long userId) {
        if (userId == null) {
            return 0;
        }
        return learningRecordRepository.findByUserIdOrderByOccurredAtDesc(userId).size();
    }

    private String resolveAdvice(JobProfile profile, List<CareerFitDTO.SkillFit> toImprove) {
        if (toImprove != null && !toImprove.isEmpty()) {
            String names = String.join("、", toImprove.stream()
                    .map(CareerFitDTO.SkillFit::getSkillName)
                    .toList());
            if (profile != null && profile.getAdvice() != null && !profile.getAdvice().isBlank()) {
                return profile.getAdvice() + "当前优先补齐：" + names + "。";
            }
            return "优先补齐：" + names + "。";
        }
        if (profile != null && profile.getAdvice() != null && !profile.getAdvice().isBlank()) {
            return profile.getAdvice();
        }
        return "岗位要求已基本达成，可以开始准备项目作品与面试。";
    }

    private String resolveDataStatus(int evidenceCount, int coveredSkills, int totalSkills) {
        if (totalSkills == 0) {
            return STATUS_INSUFFICIENT;
        }
        if (evidenceCount == 0 || coveredSkills == 0) {
            return STATUS_INSUFFICIENT;
        }
        if (coveredSkills >= READY_SKILL_THRESHOLD) {
            return STATUS_READY;
        }
        return STATUS_PARTIAL;
    }

    private String dataStatusText(String dataStatus, int evidenceCount) {
        return switch (dataStatus) {
            case STATUS_READY -> "已根据 " + evidenceCount + " 条学习记录计算，可直接参考。";
            case STATUS_PARTIAL -> "已根据 " + evidenceCount + " 条学习记录计算，继续完成课程与项目会更准确。";
            default -> "还没有可用的学习记录，先做几道题或完成课程章节后再看匹配度。";
        };
    }

    private List<String> splitResponsibilities(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("[;；]")).stream()
                .map(String::trim)
                .filter((item) -> !item.isEmpty())
                .toList();
    }

    private int clamp(Integer value) {
        int numeric = value == null ? 0 : value;
        return Math.max(0, Math.min(100, numeric));
    }
}
