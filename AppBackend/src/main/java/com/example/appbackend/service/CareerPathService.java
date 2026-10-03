package com.example.appbackend.service;

import com.example.appbackend.dto.CareerFitDTO;
import com.example.appbackend.dto.CareerPathDTO;
import com.example.appbackend.entity.CareerJobRelation;
import com.example.appbackend.entity.JobProfile;
import com.example.appbackend.repository.JobProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 职业路径图谱：把管理端配置的「岗位关系」和已有的人岗匹配结果拼成一张职业路径图。
 *
 * 说明：
 * 1. 星球的大小、坐标、图片全部来自管理端星图配置，这里不参与、也不修改；
 * 2. 匹配度、可复用技能、待补齐技能来自 job_skill_requirement + learning_record，
 *    与岗位探索页、岗位详情页共用同一套口径；
 * 3. 关系里手工填写的可复用/待补齐技能优先，留空才用自动计算结果。
 */
@Service
public class CareerPathService {

    /** 岗位名称别名：星图里的叫法与技能体系里的叫法不完全一致时用的兜底表（键为去掉空格的写法）。 */
    private static final Map<String, String> NAME_ALIASES = Map.ofEntries(
            Map.entry("java开发工程师", "java-backend"),
            Map.entry("java工程师", "java-backend"),
            Map.entry("ai应用工程师", "ai-app"),
            Map.entry("ai应用开发工程师", "ai-app"),
            Map.entry("ai算法工程师", "algorithm"),
            Map.entry("python开发工程师", "python-backend"),
            Map.entry("python工程师", "python-backend"),
            Map.entry("软件测试工程师", "software-testing"),
            Map.entry("测试开发工程师", "software-testing"),
            Map.entry("前端开发工程师", "frontend"),
            Map.entry("web前端开发工程师", "frontend"),
            Map.entry("数据分析师", "data-analyst"),
            Map.entry("产品经理", "product-manager")
    );

    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final CareerNebulaService nebulaService;
    private final CareerJobRelationService relationService;
    private final JobProfileRepository jobProfileRepository;
    private final CareerFitService careerFitService;

    public CareerPathService(CareerNebulaService nebulaService,
                             CareerJobRelationService relationService,
                             JobProfileRepository jobProfileRepository,
                             CareerFitService careerFitService) {
        this.nebulaService = nebulaService;
        this.relationService = relationService;
        this.jobProfileRepository = jobProfileRepository;
        this.careerFitService = careerFitService;
    }

    @Transactional
    public CareerPathDTO overview(Long userId) {
        Map<String, Object> map = nebulaService.getMap();
        List<Map<String, Object>> careers = enabledCareers(map);

        List<JobProfile> profiles = jobProfileRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<String, CareerFitDTO> fitCache = new LinkedHashMap<>();
        Map<String, CareerPathDTO.JobNode> nodes = new LinkedHashMap<>();
        int evidenceCount = 0;

        for (Map<String, Object> career : careers) {
            String jobId = text(career.get("id"));
            if (jobId.isEmpty()) continue;
            String name = text(career.get("name"));
            JobProfile profile = resolveProfile(career, profiles);
            CareerFitDTO fit = fitFor(profile, name, userId, fitCache);
            if (fit != null && fit.getEvidenceCount() != null) {
                evidenceCount = Math.max(evidenceCount, fit.getEvidenceCount());
            }
            nodes.put(jobId, toJobNode(jobId, name, profile, fit));
        }

        CareerPathDTO dto = new CareerPathDTO();
        dto.setJobs(new ArrayList<>(nodes.values()));
        dto.setRelations(buildRelations(userId, nodes, fitCache, profiles));
        dto.setUserData(userData(evidenceCount, dto.getJobs()));
        return dto;
    }

    /* ---------------- 岗位关系 ---------------- */

    private List<CareerPathDTO.RelationView> buildRelations(Long userId,
                                                            Map<String, CareerPathDTO.JobNode> nodes,
                                                            Map<String, CareerFitDTO> fitCache,
                                                            List<JobProfile> profiles) {
        List<CareerPathDTO.RelationView> result = new ArrayList<>();
        for (CareerJobRelation relation : relationService.listEnabled()) {
            CareerPathDTO.JobNode source = nodes.get(relation.getSourceJobId());
            CareerPathDTO.JobNode target = nodes.get(relation.getTargetJobId());
            // 关系两端必须都是当前展示的岗位，否则连不出来
            if (source == null || target == null) continue;

            CareerPathDTO.RelationView view = new CareerPathDTO.RelationView();
            view.setId(relation.getId());
            view.setSourceJobId(relation.getSourceJobId());
            view.setSourceName(source.getName());
            view.setTargetJobId(relation.getTargetJobId());
            view.setTargetName(target.getName());
            view.setRelationType(relation.getRelationType());
            view.setRelationName(relationName(relation));
            view.setDescription(relation.getDescription());
            view.setLineType(relation.getLineType());
            view.setRecommendation(relation.getRecommendation());
            view.setEnabled(relation.getEnabled());
            view.setSortOrder(relation.getSortOrder());

            JobProfile profile = resolveByJobCode(target.getJobCode(), profiles);
            CareerFitDTO fit = fitFor(profile, target.getName(), userId, fitCache);
            applyAnalysis(view, relation, fit);
            result.add(view);
        }
        result.sort(Comparator.comparing(CareerPathDTO.RelationView::getRecommendation,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private void applyAnalysis(CareerPathDTO.RelationView view, CareerJobRelation relation, CareerFitDTO fit) {
        // 管理员手工填写的技能优先，留空才用真实学习数据算出来的结果
        List<String> configuredReusable = splitSkills(relation.getReusableSkills());
        List<String> configuredMissing = splitSkills(relation.getMissingSkills());

        if (fit != null) {
            view.setJobCode(fit.getJobCode());
            view.setDirection(fit.getDirection());
            view.setType(fit.getType());
            view.setDataStatus(fit.getDataStatus());
            view.setDataStatusText(fit.getDataStatusText());
            view.setAdvice(fit.getAdvice());
            view.setTotalSkills(fit.getTotalSkills());
            if (fit.getGaps() != null) {
                view.setRequiredSkills(fit.getGaps().stream().map(CareerFitDTO.SkillFit::getSkillName).toList());
            }
        }

        boolean hasMatch = fit != null
                && fit.getMatchRate() != null
                && !CareerFitService.STATUS_INSUFFICIENT.equals(fit.getDataStatus());
        view.setHasMatch(hasMatch);
        if (hasMatch) {
            List<String> autoReusable = fit.getMastered() == null ? List.of()
                    : fit.getMastered().stream().map(CareerFitDTO.SkillFit::getSkillName).toList();
            List<String> autoMissing = fit.getGaps() == null ? List.of()
                    : fit.getGaps().stream().map(CareerFitDTO.SkillFit::getSkillName).toList();
            view.setReusableSkills(configuredReusable.isEmpty() ? autoReusable : configuredReusable);
            view.setMissingSkills(configuredMissing.isEmpty() ? autoMissing : configuredMissing);
            view.setMasteredCount(fit.getMastered() == null ? 0 : fit.getMastered().size());
            view.setMissingCount(fit.getGaps() == null ? 0 : fit.getGaps().size());
            view.setMatchRate(fit.getMatchRate());
            view.setDifficulty(difficulty(fit.getMatchRate()));
        } else {
            // 没有可用的学习记录时不编造差距，只展示管理端手工配置的技能
            view.setReusableSkills(configuredReusable);
            view.setMissingSkills(configuredMissing);
            view.setMasteredCount(null);
            view.setMissingCount(null);
            view.setMatchRate(null);
            view.setDifficulty(null);
        }
    }

    /* ---------------- 岗位节点 ---------------- */

    private CareerPathDTO.JobNode toJobNode(String jobId, String name, JobProfile profile, CareerFitDTO fit) {
        CareerPathDTO.JobNode node = new CareerPathDTO.JobNode();
        node.setJobId(jobId);
        node.setName(name.isEmpty() && profile != null ? profile.getName() : name);
        node.setJobCode(profile == null ? null : profile.getCode());
        node.setDirection(profile == null ? null : profile.getDirection());
        node.setType(profile == null ? null : profile.getType());
        boolean hasMatch = fit != null
                && fit.getMatchRate() != null
                && !CareerFitService.STATUS_INSUFFICIENT.equals(fit.getDataStatus());
        node.setHasMatch(hasMatch);
        node.setDataStatus(fit == null ? null : fit.getDataStatus());
        if (hasMatch) {
            node.setMatchRate(fit.getMatchRate());
            node.setMasteredCount(fit.getMastered() == null ? 0 : fit.getMastered().size());
            node.setMissingCount(fit.getGaps() == null ? 0 : fit.getGaps().size());
            node.setTopGap(fit.getToImprove() == null || fit.getToImprove().isEmpty()
                    ? null : fit.getToImprove().get(0).getSkillName());
        }
        return node;
    }

    private CareerPathDTO.UserDataStatus userData(int evidenceCount, List<CareerPathDTO.JobNode> jobs) {
        CareerPathDTO.UserDataStatus status = new CareerPathDTO.UserDataStatus();
        status.setEvidenceCount(evidenceCount);
        boolean anyMatch = jobs.stream().anyMatch((job) -> Boolean.TRUE.equals(job.getHasMatch()));
        status.setReady(anyMatch);
        if (anyMatch) {
            status.setStatus(CareerFitService.STATUS_READY);
            status.setText("已根据 " + evidenceCount + " 条学习记录计算岗位匹配度。");
        } else {
            status.setStatus(CareerFitService.STATUS_INSUFFICIENT);
            status.setText("当前成长画像还不完整，完善成长档案后可以获得个性化岗位迁移分析。");
        }
        return status;
    }

    /* ---------------- 岗位解析与匹配 ---------------- */

    private CareerFitDTO fitFor(JobProfile profile, String fallbackName, Long userId,
                                Map<String, CareerFitDTO> cache) {
        String key = profile != null ? profile.getCode() : fallbackName;
        if (key == null || key.isBlank()) {
            return null;
        }
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        CareerFitDTO fit;
        try {
            fit = careerFitService.jobFit(profile != null ? profile.getName() : fallbackName, userId);
        } catch (RuntimeException error) {
            fit = null;
        }
        cache.put(key, fit);
        return fit;
    }

    private JobProfile resolveProfile(Map<String, Object> career, List<JobProfile> profiles) {
        String explicit = text(career.get("jobCode"));
        if (!explicit.isEmpty()) {
            JobProfile bound = resolveByJobCode(explicit, profiles);
            if (bound != null) return bound;
        }
        String jobId = text(career.get("id"));
        String name = text(career.get("name"));

        JobProfile byId = resolveByJobCode(jobId, profiles);
        if (byId != null) return byId;
        if (name.isEmpty()) return null;

        for (JobProfile profile : profiles) {
            if (name.equalsIgnoreCase(profile.getName())) return profile;
        }
        String normalized = normalize(name);
        String aliasCode = NAME_ALIASES.get(normalized);
        if (aliasCode != null) {
            JobProfile aliased = resolveByJobCode(aliasCode, profiles);
            if (aliased != null) return aliased;
        }
        for (JobProfile profile : profiles) {
            if (normalize(profile.getName()).equals(normalized)) return profile;
        }
        JobProfile best = null;
        double bestScore = SIMILARITY_THRESHOLD;
        for (JobProfile profile : profiles) {
            double score = similarity(normalized, normalize(profile.getName()));
            if (score > bestScore) {
                bestScore = score;
                best = profile;
            }
        }
        return best;
    }

    private JobProfile resolveByJobCode(String code, List<JobProfile> profiles) {
        if (code == null || code.isBlank()) return null;
        String value = code.trim();
        return profiles.stream()
                .filter((profile) -> value.equalsIgnoreCase(profile.getCode()))
                .findFirst()
                .orElse(null);
    }

    /* ---------------- 工具方法 ---------------- */

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> enabledCareers(Map<String, Object> map) {
        Object raw = map == null ? null : map.get("careers");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> entry) {
                Map<String, Object> row = (Map<String, Object>) entry;
                String status = text(row.get("status"));
                if (status.isEmpty() || "enabled".equalsIgnoreCase(status)) {
                    result.add(row);
                }
            }
        }
        return result;
    }

    private String relationName(CareerJobRelation relation) {
        if (relation.getRelationName() != null && !relation.getRelationName().isBlank()) {
            return relation.getRelationName().trim();
        }
        return switch (relation.getRelationType()) {
            case CareerJobRelation.TYPE_PROMOTION -> "职业进阶";
            case CareerJobRelation.TYPE_TRANSFER -> "横向转岗";
            case CareerJobRelation.TYPE_BRANCH -> "发展分支";
            default -> "相近岗位";
        };
    }

    private String difficulty(Integer matchRate) {
        if (matchRate == null) return null;
        if (matchRate >= 75) return "低";
        if (matchRate >= 55) return "中";
        return "高";
    }

    private List<String> splitSkills(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String part : raw.split("[、,，/;；|]")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return new ArrayList<>(values);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\s·/（）()\\-]+", "");
    }

    /** 字符二元组 Jaccard 相似度：只用于星图岗位名和技能体系岗位名对不上时的兜底匹配。 */
    private double similarity(String left, String right) {
        if (left.isEmpty() || right.isEmpty()) return 0;
        Set<String> a = bigrams(left);
        Set<String> b = bigrams(right);
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> intersection = new LinkedHashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new LinkedHashSet<>(a);
        union.addAll(b);
        return intersection.size() / (double) union.size();
    }

    private Set<String> bigrams(String value) {
        Set<String> grams = new LinkedHashSet<>();
        for (int i = 0; i + 1 < value.length(); i += 1) {
            grams.add(value.substring(i, i + 2));
        }
        return grams;
    }
}
