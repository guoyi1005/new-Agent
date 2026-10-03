package com.example.appbackend.repository;

import com.example.appbackend.entity.CampusCourseExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CampusCourseExamRepository extends JpaRepository<CampusCourseExam, Long> {
    List<CampusCourseExam> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);
    Optional<CampusCourseExam> findByCourseIdAndPaperId(Long courseId, Long paperId);

    /** 管理端：按试卷 ID 反查它作为「课程考试」挂在哪些课程下。 */
    List<CampusCourseExam> findByPaperIdIn(Collection<Long> paperIds);
    void deleteByCourseId(Long courseId);
}
