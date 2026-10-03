package com.example.appbackend.repository;

import com.example.appbackend.entity.JobProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobProfileRepository extends JpaRepository<JobProfile, Long> {

    Optional<JobProfile> findByCode(String code);

    Optional<JobProfile> findByName(String name);

    List<JobProfile> findByStatusOrderBySortOrderAscIdAsc(String status);

    List<JobProfile> findAllByOrderBySortOrderAscIdAsc();
}
