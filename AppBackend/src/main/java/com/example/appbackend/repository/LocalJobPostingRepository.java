package com.example.appbackend.repository;

import com.example.appbackend.entity.LocalJobPosting;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LocalJobPostingRepository extends JpaRepository<LocalJobPosting, Long> {

    Optional<LocalJobPosting> findByFingerprint(String fingerprint);

    long countByPublishedAtGreaterThanEqual(LocalDateTime time);

    long countByJobCategory(String jobCategory);

    long countByLastSeenAtGreaterThanEqual(LocalDateTime time);

    @Query("select count(p) from LocalJobPosting p where "
            + "(p.publishedAt is not null and p.publishedAt >= :start) "
            + "or (p.publishedAt is null and p.firstSeenAt >= :start)")
    long countNewSince(@Param("start") LocalDateTime start);

    @Query("select max(p.lastSeenAt) from LocalJobPosting p")
    LocalDateTime findLastCrawledAt();

    List<LocalJobPosting> findAllByOrderByPublishedAtDescIdDesc(Pageable pageable);

    List<LocalJobPosting> findByJobTitleContainingIgnoreCaseOrderByPublishedAtDescIdDesc(String keyword, Pageable pageable);

    @Query("select p.district as name, count(p) as total from LocalJobPosting p "
            + "where p.district is not null and p.district <> '' "
            + "group by p.district order by count(p) desc")
    List<NameCount> countGroupByDistrict(Pageable pageable);

    @Query("select p.sourceName as name, count(p) as total from LocalJobPosting p "
            + "group by p.sourceName order by count(p) desc")
    List<NameCount> countGroupBySource();

    @Query("select distinct p.sourceKeys from LocalJobPosting p where p.sourceKeys is not null")
    List<String> findDistinctSourceKeys();

    @Query("select p from LocalJobPosting p where p.fingerprint in :fingerprints")
    List<LocalJobPosting> findAllByFingerprintIn(@Param("fingerprints") Collection<String> fingerprints);

    /** 聚合查询的投影：只取名称和数量。 */
    interface NameCount {
        String getName();

        long getTotal();
    }
}
