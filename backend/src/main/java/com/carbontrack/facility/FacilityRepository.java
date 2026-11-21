package com.carbontrack.facility;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    List<Facility> findByCompanyIdOrderByIdAsc(Long companyId);

    /** Always look facilities up through the company so another company's id behaves as not found. */
    Optional<Facility> findByIdAndCompanyId(Long id, Long companyId);
}
