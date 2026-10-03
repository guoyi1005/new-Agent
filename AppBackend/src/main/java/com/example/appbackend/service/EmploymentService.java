package com.example.appbackend.service;

import com.example.appbackend.dto.CampusRecruitmentSummaryDTO;
import com.example.appbackend.entity.AlumniEnterprise;
import com.example.appbackend.entity.CampusRecruitment;

import java.util.List;

public interface EmploymentService {

    // ========== 校友企业 ==========

    List<AlumniEnterprise> listAlumni(String keyword);

    List<AlumniEnterprise> listPublishedAlumni();

    AlumniEnterprise createAlumni(AlumniEnterprise body);

    AlumniEnterprise updateAlumni(Long id, AlumniEnterprise body);

    void deleteAlumni(Long id);

    // ========== 校园招聘 ==========

    List<CampusRecruitment> listCampusRecruitments(String keyword);

    List<CampusRecruitment> listPublishedCampusRecruitments();

    CampusRecruitmentSummaryDTO campusRecruitmentSummary();

    CampusRecruitment createCampusRecruitment(CampusRecruitment body);

    CampusRecruitment updateCampusRecruitment(Long id, CampusRecruitment body);

    void deleteCampusRecruitment(Long id);
}
