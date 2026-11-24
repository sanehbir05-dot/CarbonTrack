package com.carbontrack.factor;

import com.carbontrack.common.Category;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmissionFactorRepository extends JpaRepository<EmissionFactor, Long> {

    List<EmissionFactor> findAllByOrderByCategoryAscRegionAscYearAsc();

    List<EmissionFactor> findByYearOrderByCategoryAscRegionAsc(Integer year);

    List<EmissionFactor> findByCategoryAndRegionIn(Category category, Collection<String> regions);
}
