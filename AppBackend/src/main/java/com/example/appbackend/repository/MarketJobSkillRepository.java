package com.example.appbackend.repository;

import com.example.appbackend.entity.MarketJobSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MarketJobSkillRepository extends JpaRepository<MarketJobSkill, Long> {
    void deleteByJobId(Long jobId);
    List<MarketJobSkill> findByJobIdIn(Collection<Long> jobIds);
    List<MarketJobSkill> findByJobId(Long jobId);
}
