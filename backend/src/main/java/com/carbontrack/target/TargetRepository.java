package com.carbontrack.target;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TargetRepository extends JpaRepository<Target, Long> {

    List<Target> findByCompanyIdOrderByIdAsc(Long companyId);

    Optional<Target> findByIdAndCompanyId(Long id, Long companyId);
}
