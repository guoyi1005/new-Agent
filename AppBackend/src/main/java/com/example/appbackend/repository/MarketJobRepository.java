package com.example.appbackend.repository;

import com.example.appbackend.entity.MarketJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketJobRepository extends JpaRepository<MarketJob, Long> {
    java.util.Optional<MarketJob> findBySourceAndSourceJobId(String source, String sourceJobId);
    java.util.Optional<MarketJob> findByFingerprint(String fingerprint);
    List<MarketJob> findByStatusAndJobTypeIgnoreCaseOrderByPublishTimeDesc(String status, String jobType);
    List<MarketJob> findByStatusOrderByPublishTimeDesc(String status);
    List<MarketJob> findByStatusAndCityIgnoreCaseOrderByPublishTimeDesc(String status, String city);
}
