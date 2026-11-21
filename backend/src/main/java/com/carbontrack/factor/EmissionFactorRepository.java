package com.carbontrack.factor;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmissionFactorRepository extends JpaRepository<EmissionFactor, Long> {

    List<EmissionFactor> findAllByOrderByCategoryAscRegionAscYearAsc();

    List<EmissionFactor> findByYearOrderByCategoryAscRegionAsc(Integer year);
}
