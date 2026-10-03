package com.example.appbackend.service;

import com.example.appbackend.dto.CampusRecruitmentSummaryDTO;
import com.example.appbackend.entity.AlumniEnterprise;
import com.example.appbackend.entity.CampusRecruitment;
import com.example.appbackend.entity.Result;
import com.example.appbackend.exception.BusinessException;
import com.example.appbackend.repository.AlumniEnterpriseRepository;
import com.example.appbackend.repository.CampusRecruitmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class EmploymentServiceImpl implements EmploymentService {

    private final AlumniEnterpriseRepository alumniRepository;
    private final CampusRecruitmentRepository campusRepository;

    public EmploymentServiceImpl(AlumniEnterpriseRepository alumniRepository,
                                 CampusRecruitmentRepository campusRepository) {
        this.alumniRepository = alumniRepository;
        this.campusRepository = campusRepository;
    }

    // ========== 校友企业 ==========

    @Override
    public List<AlumniEnterprise> listAlumni(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return alumniRepository.findAllByOrderBySortOrderAscIdAsc();
        }
        return alumniRepository.findByNameContainingOrderBySortOrderAscIdAsc(keyword.trim());
    }

    @Override
    public List<AlumniEnterprise> listPublishedAlumni() {
        return alumniRepository.findByEnabledTrueOrderBySortOrderAscIdAsc();
    }

    @Override
    @Transactional
    public AlumniEnterprise createAlumni(AlumniEnterprise body) {
        if (body.getName() == null || body.getName().isBlank()) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "请填写企业名称");
        }
        body.setId(null);
        return alumniRepository.save(body);
    }

    @Override
    @Transactional
    public AlumniEnterprise updateAlumni(Long id, AlumniEnterprise body) {
        AlumniEnterprise entity = alumniRepository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "校友企业不存在"));
        if (body.getName() != null && !body.getName().isBlank()) {
            entity.setName(body.getName().trim());
        }
        entity.setShortName(body.getShortName());
        entity.setIndustry(body.getIndustry());
        entity.setFields(body.getFields());
        entity.setAlumniCount(body.getAlumniCount() == null ? 0 : body.getAlumniCount());
        entity.setHiring(body.getHiring() == null ? Boolean.TRUE : body.getHiring());
        entity.setOpenPositions(body.getOpenPositions() == null ? 0 : body.getOpenPositions());
        entity.setContactName(body.getContactName());
        entity.setContactPhone(body.getContactPhone());
        entity.setDescription(body.getDescription());
        entity.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        entity.setEnabled(body.getEnabled() == null ? Boolean.TRUE : body.getEnabled());
        return alumniRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteAlumni(Long id) {
        if (!alumniRepository.existsById(id)) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "校友企业不存在");
        }
        alumniRepository.deleteById(id);
    }

    // ========== 校园招聘 ==========

    @Override
    public List<CampusRecruitment> listCampusRecruitments(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return campusRepository.findAllByOrderBySortOrderAscIdAsc();
        }
        return campusRepository.findByTitleContainingOrderBySortOrderAscIdAsc(keyword.trim());
    }

    @Override
    public List<CampusRecruitment> listPublishedCampusRecruitments() {
        return campusRepository.findByStatusOrderBySortOrderAscIdAsc(CampusRecruitment.STATUS_PUBLISHED);
    }

    @Override
    public CampusRecruitmentSummaryDTO campusRecruitmentSummary() {
        List<CampusRecruitment> items = listPublishedCampusRecruitments();

        CampusRecruitmentSummaryDTO summary = new CampusRecruitmentSummaryDTO();
        summary.setTalks((int) items.stream()
                .filter((item) -> CampusRecruitment.TYPE_TALK.equals(item.getType()))
                .count());
        summary.setFairs((int) items.stream()
                .filter((item) -> CampusRecruitment.TYPE_FAIR.equals(item.getType()))
                .count());
        summary.setRoles(items.stream()
                .map((item) -> item.getRoleCount() == null ? 0 : item.getRoleCount())
                .reduce(0, Integer::sum));
        summary.setSeason(items.stream()
                .map(CampusRecruitment::getSeason)
                .filter((season) -> season != null && !season.isBlank())
                .findFirst()
                .orElse(null));

        LocalDate today = LocalDate.now();
        summary.setSchedule(items.stream()
                .filter((item) -> item.getEventDate() != null)
                .filter((item) -> !item.getEventDate().isBefore(today))
                .sorted(Comparator.comparing(CampusRecruitment::getEventDate))
                .limit(3)
                .map((item) -> new CampusRecruitmentSummaryDTO.ScheduleItem(
                        item.getEventDate().toString(),
                        item.getTitle()))
                .toList());
        return summary;
    }

    @Override
    @Transactional
    public CampusRecruitment createCampusRecruitment(CampusRecruitment body) {
        if (body.getTitle() == null || body.getTitle().isBlank()) {
            throw new BusinessException(Result.BAD_REQUEST_CODE, "请填写活动或岗位名称");
        }
        body.setId(null);
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus(CampusRecruitment.STATUS_PUBLISHED);
        }
        if (body.getType() == null || body.getType().isBlank()) {
            body.setType(CampusRecruitment.TYPE_TALK);
        }
        return campusRepository.save(body);
    }

    @Override
    @Transactional
    public CampusRecruitment updateCampusRecruitment(Long id, CampusRecruitment body) {
        CampusRecruitment entity = campusRepository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND_CODE, "校园招聘条目不存在"));
        if (body.getTitle() != null && !body.getTitle().isBlank()) {
            entity.setTitle(body.getTitle().trim());
        }
        entity.setCompany(body.getCompany());
        if (body.getType() != null && !body.getType().isBlank()) {
            entity.setType(body.getType());
        }
        entity.setSeason(body.getSeason());
        entity.setCity(body.getCity());
        entity.setLocation(body.getLocation());
        entity.setEventDate(body.getEventDate());
        entity.setRoleCount(body.getRoleCount() == null ? 0 : body.getRoleCount());
        entity.setDescription(body.getDescription());
        if (body.getStatus() != null && !body.getStatus().isBlank()) {
            entity.setStatus(body.getStatus());
        }
        entity.setSortOrder(body.getSortOrder() == null ? 0 : body.getSortOrder());
        return campusRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteCampusRecruitment(Long id) {
        if (!campusRepository.existsById(id)) {
            throw new BusinessException(Result.NOT_FOUND_CODE, "校园招聘条目不存在");
        }
        campusRepository.deleteById(id);
    }
}
