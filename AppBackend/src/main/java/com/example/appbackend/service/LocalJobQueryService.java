package com.example.appbackend.service;

import com.example.appbackend.dto.LocalJobPostingDTO;
import com.example.appbackend.dto.LocalJobSummaryDTO;
import com.example.appbackend.entity.LocalJobPosting;
import com.example.appbackend.repository.LocalJobPostingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 读取本地岗位表，给「成都本地就业」板块提供统计与列表。 */
@Service
public class LocalJobQueryService {

    private static final int DEFAULT_DISTRICT_LIMIT = 4;

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
        return list(page, size, null);
    }

    /** 带关键词时按岗位名过滤，用于「为你推荐的岗位」按目标方向取岗。 */
    public List<LocalJobPostingDTO> list(int page, int size, String keyword) {
        int safePage = Math.max(0, page - 1);
        int safeSize = Math.min(100, Math.max(1, size));
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        List<LocalJobPosting> rows = keyword == null || keyword.isBlank()
                ? repository.findAllByOrderByPublishedAtDescIdDesc(pageable)
                : repository.findByJobTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(keyword.trim(), pageable);
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

    private static List<LocalJobSummaryDTO.CountItem> toCountItems(
            List<LocalJobPostingRepository.NameCount> rows) {
        List<LocalJobSummaryDTO.CountItem> items = new ArrayList<>();
        for (LocalJobPostingRepository.NameCount row : rows) {
            items.add(new LocalJobSummaryDTO.CountItem(row.getName(), row.getTotal()));
        }
        return items;
    }
}
