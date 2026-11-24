package com.carbontrack.factor;

import com.carbontrack.factor.dto.FactorDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FactorService {

    private final EmissionFactorRepository factors;

    public FactorService(EmissionFactorRepository factors) {
        this.factors = factors;
    }

    /** All factors, or only those published for the given year. */
    @Transactional(readOnly = true)
    public List<FactorDto> list(Integer year) {
        List<EmissionFactor> rows = year == null
                ? factors.findAllByOrderByCategoryAscRegionAscYearAsc()
                : factors.findByYearOrderByCategoryAscRegionAsc(year);
        return rows.stream().map(FactorDto::from).toList();
    }
}
