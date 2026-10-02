package com.example.appbackend.repository;

import com.example.appbackend.entity.ExternalCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalCourseRepository extends JpaRepository<ExternalCourse, Long> {
    List<ExternalCourse> findByStatusOrderBySortOrderAscIdAsc(String status);
    Optional<ExternalCourse> findByTitle(String title);
}