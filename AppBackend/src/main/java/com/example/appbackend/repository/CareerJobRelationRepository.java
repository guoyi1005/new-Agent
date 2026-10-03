package com.example.appbackend.repository;

import com.example.appbackend.entity.CareerJobRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CareerJobRelationRepository extends JpaRepository<CareerJobRelation, Long> {

    List<CareerJobRelation> findAllByOrderBySortOrderAscIdAsc();

    List<CareerJobRelation> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<CareerJobRelation> findBySourceJobIdAndEnabledTrueOrderBySortOrderAscIdAsc(String sourceJobId);

    long countBySourceJobIdAndTargetJobId(String sourceJobId, String targetJobId);
}
