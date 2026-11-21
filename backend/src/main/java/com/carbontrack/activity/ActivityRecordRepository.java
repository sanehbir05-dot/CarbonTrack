package com.carbontrack.activity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ActivityRecordRepository
        extends JpaRepository<ActivityRecord, Long>, JpaSpecificationExecutor<ActivityRecord> {

    /** Facility and factor are loaded together so mapping to a DTO needs no extra queries. */
    @Override
    @EntityGraph(attributePaths = {"facility", "factor"})
    Page<ActivityRecord> findAll(Specification<ActivityRecord> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"facility", "factor"})
    Optional<ActivityRecord> findByIdAndCompanyId(Long id, Long companyId);

    @Query("select min(r.year) from ActivityRecord r where r.companyId = :companyId")
    Integer findMinYear(Long companyId);
}
