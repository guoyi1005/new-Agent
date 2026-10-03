package com.example.appbackend.repository;

import com.example.appbackend.entity.PythonPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PythonPaperRepository extends JpaRepository<PythonPaper, Long> {
    List<PythonPaper> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    Optional<PythonPaper> findByIdAndUserId(Long id, Long userId);
}
