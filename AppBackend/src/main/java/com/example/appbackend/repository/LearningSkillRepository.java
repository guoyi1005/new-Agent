package com.example.appbackend.repository;
import com.example.appbackend.entity.LearningSkill; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface LearningSkillRepository extends JpaRepository<LearningSkill,Long>{ Optional<LearningSkill> findByCode(String code); List<LearningSkill> findAllByStatusOrderBySortOrderAscIdAsc(String status); }