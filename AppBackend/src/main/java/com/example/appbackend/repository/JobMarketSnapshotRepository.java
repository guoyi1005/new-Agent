package com.example.appbackend.repository;

import com.example.appbackend.entity.JobMarketSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JobMarketSnapshotRepository extends JpaRepository<JobMarketSnapshot, Long> {
    java.util.Optional<JobMarketSnapshot> findFirstByOrderBySnapshotDateDesc();
    List<JobMarketSnapshot> findBySnapshotDateOrderByHotScoreDesc(LocalDate snapshotDate);
    List<JobMarketSnapshot> findBySnapshotDateAndCityIgnoreCaseOrderByHotScoreDesc(LocalDate snapshotDate, String city);
    List<JobMarketSnapshot> findBySnapshotDateAndJobTypeIgnoreCaseOrderByHotScoreDesc(LocalDate snapshotDate, String jobType);
    List<JobMarketSnapshot> findBySnapshotDateAndCityIgnoreCaseAndJobTypeIgnoreCaseOrderByHotScoreDesc(LocalDate snapshotDate, String city, String jobType);
    java.util.Optional<JobMarketSnapshot> findBySnapshotDateAndNormalizedTitleAndCityAndJobType(LocalDate date, String title, String city, String jobType);
    java.util.Optional<JobMarketSnapshot> findFirstByNormalizedTitleAndCityAndJobTypeOrderBySnapshotDateDesc(String title, String city, String jobType);
}
