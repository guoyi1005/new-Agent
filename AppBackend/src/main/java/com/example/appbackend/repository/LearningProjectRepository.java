package com.example.appbackend.repository;

import com.example.appbackend.entity.LearningProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningProjectRepository extends JpaRepository<LearningProject, Long> {
    Optional<LearningProject> findByCode(String code);
    List<LearningProject> findByStatusOrderBySortOrderAscIdAsc(String status);
    List<LearningProject> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByCode(String code);
}
