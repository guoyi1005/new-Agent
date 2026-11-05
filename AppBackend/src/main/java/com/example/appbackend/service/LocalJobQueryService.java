package com.example.appbackend.service;

import com.example.appbackend.dto.LocalJobPostingDTO;
import com.example.appbackend.dto.LocalJobSummaryDTO;
import com.example.appbackend.entity.LocalJobPosting;
import com.example.appbackend.repository.LocalJobPostingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 读取本地岗位表，给「成都本地就业」板块提供统计与列表。 */
@Service
public class LocalJobQueryService {

    private static final int DEFAULT_DISTRICT_LIMIT = 4;
    /** 按目标岗位筛选时，最多在内存里评分这么多条候选（当前表只有几百条，足够）。 */
    private static final int DIRECTION_CANDIDATE_LIMIT = 500;

    /** 软件/IT/数据方向的岗位关键词：命中任何一个才算「和目标岗位沾边」。 */
    private static final List<String> SOFTWARE_KEYWORDS = List.of(
            "开发", "软件", "程序", "编程", "后端", "前端", "全栈", "web", "java", "python",
            "c++", "golang", "数据", "算法", "测试", "运维", "网络", "it", "系统", "计算机",
            "互联网", "信息化", "数字化", "人工智能", "ai", "大模型", "视觉", "通信",
            "自动化", "嵌入式", "单片机", "小程序", "网站", "技术支持", "安全", "技术");

    /** 明确与软件岗位无关的服务/蓝领/职能岗，直接排除。 */
    private static final List<String> NON_TECH_KEYWORDS = List.of(
            "服务员", "客房", "前台", "接待", "保安", "保洁", "清洁", "洗碗", "厨师", "餐饮",
            "普工", "操作工", "司机", "仓库", "仓管", "物流", "快递", "销售", "客服", "收银",
            "导购", "店员", "门店", "护理", "保姆", "月嫂", "绿化", "物业", "家政", "理货",
            "搬运", "焊工", "电工", "钳工", "铆工", "叉车", "cnc", "机加工", "安装工", "维修",
            "宣传", "媒体", "营销", "主播", "直播", "带货", "文员", "行政", "人事", "会计",
            "财务", "生产", "技工");

    private final LocalJobPostingRepository repository;

    public LocalJobQueryService(LocalJobPostingRepository repository) {
        this.repository = repository;
    }

    public LocalJobSummaryDTO summary() {
        LocalJobSummaryDTO summary = new LocalJobSummaryDTO();
        summary.setTotal(repository.count());
        summary.setTodayNew(repository.countNewSince(LocalDate.now().atStartOfDay()));
        summary.setIntern(repository.countByJobCategory("intern"));
        summary.setCampus(repository.countByJobCategory("campus"));
        summary.setStateOwned(repository.countByJobCategory("stateOwned"));
        summary.setUpdatedAt(repository.findLastCrawledAt());
        summary.setDistricts(toCountItems(repository.countGroupByDistrict(PageRequest.of(0, DEFAULT_DISTRICT_LIMIT))));
        summary.setSources(toCountItems(repository.countGroupBySource()));
        Set<String> platforms = new LinkedHashSet<>();
        for (String keys : repository.findDistinctSourceKeys()) {
            for (String key : keys.split(",")) {
                String trimmed = key.trim();
                if (!trimmed.isEmpty()) {
                    platforms.add(trimmed);
                }
            }
        }
        summary.setPlatforms(new ArrayList<>(platforms));
        return summary;
    }

    public List<LocalJobPostingDTO> list(int page, int size) {
        return list(page, size, null, null);
    }

    /** 带关键词时按岗位名过滤，用于「为你推荐的岗位」按目标方向取岗。 */
    public List<LocalJobPostingDTO> list(int page, int size, String keyword) {
        return list(page, size, keyword, null);
    }

    /**
     * @param keyword   岗位名包含匹配（可选）
     * @param direction 目标岗位名称；传了就只返回与之相关、并按相关度排好序的岗位
     */
    public List<LocalJobPostingDTO> list(int page, int size, String keyword, String direction) {
        int safePage = Math.max(0, page - 1);
        int safeSize = Math.min(100, Math.max(1, size));
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        List<LocalJobPosting> rows;
        if (direction != null && !direction.isBlank()) {
            // 站点最新页以普工/服务员为主，按「发布时间倒序取前 N 条」再过滤，
            // 否则目标方向相关的岗位永远排不进前几条。
            List<LocalJobPosting> candidates = new ArrayList<>(repository
                    .findAllByOrderByPublishedAtDescIdDesc(PageRequest.of(0, DIRECTION_CANDIDATE_LIMIT)));
            if (keyword != null && !keyword.isBlank()) {
                String needle = keyword.trim().toLowerCase(Locale.ROOT);
                candidates.removeIf(posting -> posting.getJobTitle() == null
                        || !posting.getJobTitle().toLowerCase(Locale.ROOT).contains(needle));
            }
            List<ScoredPosting> scored = new ArrayList<>();
            for (LocalJobPosting posting : candidates) {
                int score = relevanceScore(posting, direction);
                if (score > 0) {
                    scored.add(new ScoredPosting(posting, score));
                }
            }
            scored.sort(Comparator.comparingInt(ScoredPosting::score).reversed()
                    .thenComparing(item -> item.posting().getPublishedAt(),
                            Comparator.nullsLast(Comparator.reverseOrder())));
            if (scored.isEmpty()) {
                // 一条相关的都没有时退回原始列表，避免整个板块消失
                rows = repository.findAllByOrderByPublishedAtDescIdDesc(pageable);
            } else {
                int from = Math.min(scored.size(), safePage * safeSize);
                int to = Math.min(scored.size(), from + safeSize);
                rows = scored.subList(from, to).stream().map(ScoredPosting::posting).toList();
            }
        } else {
            rows = keyword == null || keyword.isBlank()
                    ? repository.findAllByOrderByPublishedAtDescIdDesc(pageable)
                    : repository.findByJobTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(keyword.trim(), pageable);
        }

        List<LocalJobPostingDTO> result = new ArrayList<>();
        for (LocalJobPosting posting : rows) {
            LocalJobPostingDTO dto = new LocalJobPostingDTO();
            dto.setId(posting.getId());
            dto.setJobTitle(posting.getJobTitle());
            dto.setCompany(posting.getCompany());
            dto.setCity(posting.getCity());
            dto.setDistrict(posting.getDistrict());
            dto.setSalaryText(posting.getSalaryText());
            dto.setEducation(posting.getEducation());
            dto.setJobCategory(posting.getJobCategory());
            dto.setSourceName(posting.getSourceName());
            dto.setSourceKeys(posting.getSourceKeys());
            dto.setDetailUrl(posting.getDetailUrl());
            dto.setPublishedAt(posting.getPublishedAt());
            dto.setLastSeenAt(posting.getLastSeenAt());
            result.add(dto);
        }
        return result;
    }

    /** 0 表示不相关；分数越高越贴近目标岗位，用于排序。 */
    private int relevanceScore(LocalJobPosting posting, String direction) {
        String title = posting.getJobTitle() == null
                ? "" : posting.getJobTitle().toLowerCase(Locale.ROOT);
        if (title.isBlank()) {
            return 0;
        }
        for (String bad : NON_TECH_KEYWORDS) {
            if (title.contains(bad)) {
                return 0;
            }
        }
        int score = 0;
        for (String key : directionKeywords(direction)) {
            if (title.contains(key)) {
                score += 6;
            }
        }
        if (containsAny(title, List.of("开发", "软件", "程序", "编程"))) {
            score += 4;
        }
        if (containsAny(title, SOFTWARE_KEYWORDS)) {
            score += 3;
        }
        if (title.contains("工程师") || title.contains("技术")) {
            score += 1;
        }
        if ("intern".equalsIgnoreCase(posting.getJobCategory())) {
            score += 2;
        }
        LocalDateTime published = posting.getPublishedAt();
        if (published != null && published.isAfter(LocalDateTime.now().minusDays(7))) {
            score += 1;
        }
        return score;
    }

    /** 由目标岗位名推导方向词，例如 Python 开发工程师 -> python/后端/服务端/数据… */
    private List<String> directionKeywords(String direction) {
        String value = direction.toLowerCase(Locale.ROOT);
        List<String> keys = new ArrayList<>();
        if (value.contains("python")) {
            keys.addAll(List.of("python", "后端", "服务端", "django", "fastapi", "flask", "爬虫", "数据"));
        }
        if (value.contains("java")) {
            keys.addAll(List.of("java", "后端", "服务端", "spring", "微服务", "全栈"));
        }
        if (value.contains("前端") || value.contains("web")) {
            keys.addAll(List.of("前端", "web", "h5", "vue", "react", "javascript", "网页", "全栈"));
        }
        if (value.contains("测试")) {
            keys.addAll(List.of("测试", "qa", "自动化", "质量"));
        }
        if (value.contains("数据")) {
            keys.addAll(List.of("数据", "sql", "bi", "分析", "报表", "数仓"));
        }
        if (value.contains("算法") || value.contains("ai") || value.contains("大模型")) {
            keys.addAll(List.of("算法", "ai", "人工智能", "机器学习", "深度学习", "大模型", "视觉", "nlp"));
        }
        if (value.contains("产品")) {
            keys.addAll(List.of("产品", "需求", "运营", "用户"));
        }
        if (value.contains("运维")) {
            keys.addAll(List.of("运维", "网络", "系统", "linux", "技术支持", "安全", "it"));
        }
        if (keys.isEmpty()) {
            keys.addAll(SOFTWARE_KEYWORDS);
        }
        return keys;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static List<LocalJobSummaryDTO.CountItem> toCountItems(
            List<LocalJobPostingRepository.NameCount> rows) {
        List<LocalJobSummaryDTO.CountItem> items = new ArrayList<>();
        for (LocalJobPostingRepository.NameCount row : rows) {
            items.add(new LocalJobSummaryDTO.CountItem(row.getName(), row.getTotal()));
        }
        return items;
    }

    private record ScoredPosting(LocalJobPosting posting, int score) {
    }
}
