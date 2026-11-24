package com.carbontrack.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.Category;
import com.carbontrack.factor.EmissionFactor;
import com.carbontrack.factor.EmissionFactorRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FactorBasedEmissionCalculatorTest {

    @Mock EmissionFactorRepository repo;
    @InjectMocks FactorBasedEmissionCalculator calculator;

    private static EmissionFactor factor(long id, String region, int year, String value) {
        EmissionFactor f = new EmissionFactor();
        ReflectionTestUtils.setField(f, "id", id);
        f.setCategory(Category.ELECTRICITY);
        f.setRegion(region);
        f.setYear(year);
        f.setValueKgCo2ePerUnit(new BigDecimal(value));
        return f;
    }

    private void factorsAre(EmissionFactor... fs) {
        when(repo.findByCategoryAndRegionIn(eq(Category.ELECTRICITY), any())).thenReturn(List.of(fs));
    }

    private long chosenId(int activityYear) {
        return calculator.calculate(Category.ELECTRICITY, BigDecimal.ONE, activityYear).factor().getId();
    }

    @Test
    void multipliesQuantityByFactorValue() {
        factorsAre(factor(1, "IN", 2023, "0.710000"));

        CalculationResult r = calculator.calculate(Category.ELECTRICITY, new BigDecimal("1000"), 2023);

        assertEquals(new BigDecimal("710.000"), r.co2eKg());
        assertEquals(1L, r.factor().getId());
    }

    @Test
    void picksLatestFactorYearAtOrBeforeActivityYear() {
        factorsAre(factor(1, "IN", 2022, "0.8"), factor(2, "IN", 2024, "0.7"), factor(3, "IN", 2026, "0.6"));

        assertEquals(2L, chosenId(2025));
        assertEquals(2L, chosenId(2024));
        assertEquals(1L, chosenId(2023));
    }

    @Test
    void usesLatestFactorWhenActivityIsNewerThanAll() {
        factorsAre(factor(1, "IN", 2022, "0.8"), factor(2, "IN", 2024, "0.7"));

        assertEquals(2L, chosenId(2030));
    }

    @Test
    void usesEarliestFactorWhenActivityIsOlderThanAll() {
        factorsAre(factor(1, "IN", 2024, "0.7"), factor(2, "IN", 2022, "0.8"), factor(3, "IN", 2026, "0.6"));

        assertEquals(2L, chosenId(2019));
    }

    @Test
    void prefersIndiaOverGlobalInTheSameYear() {
        factorsAre(factor(1, "GLOBAL", 2024, "0.5"), factor(2, "IN", 2024, "0.7"));

        assertEquals(2L, chosenId(2024));
    }

    @Test
    void prefersIndiaOverGlobalInTheEarliestFallbackYear() {
        factorsAre(factor(1, "GLOBAL", 2024, "0.5"), factor(2, "IN", 2024, "0.7"));

        assertEquals(2L, chosenId(2010));
    }

    @Test
    void fallsBackToGlobalWhenNoIndiaFactorExists() {
        factorsAre(factor(1, "GLOBAL", 2024, "0.5"));

        assertEquals(1L, chosenId(2025));
    }

    @Test
    void newerGlobalYearBeatsOlderIndiaYear() {
        // Year is decided first, region only breaks ties within a year.
        factorsAre(factor(1, "IN", 2023, "0.7"), factor(2, "GLOBAL", 2025, "0.5"));

        assertEquals(2L, chosenId(2025));
        assertEquals(1L, chosenId(2024));
    }

    @Test
    void roundsToThreeDecimalsHalfUp() {
        factorsAre(factor(1, "IN", 2024, "0.333333"));

        // 1.5 x 0.333333 = 0.4999995 -> 0.500
        assertEquals(new BigDecimal("0.500"),
                calculator.calculate(Category.ELECTRICITY, new BigDecimal("1.5"), 2024).co2eKg());
        // 1 x 0.333333 = 0.333333 -> 0.333
        assertEquals(new BigDecimal("0.333"),
                calculator.calculate(Category.ELECTRICITY, BigDecimal.ONE, 2024).co2eKg());
    }

    @Test
    void throwsBadRequestWhenCategoryHasNoFactor() {
        when(repo.findByCategoryAndRegionIn(eq(Category.LPG), any())).thenReturn(List.of());

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> calculator.calculate(Category.LPG, BigDecimal.TEN, 2024));
        assertEquals("No emission factor is available for category LPG", e.getMessage());
    }
}
