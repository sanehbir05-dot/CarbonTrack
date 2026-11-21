package com.carbontrack.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbontrack.activity.dto.ActivityDto;
import com.carbontrack.activity.dto.ActivityRequest;
import com.carbontrack.calculation.CalculationResult;
import com.carbontrack.calculation.EmissionCalculator;
import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.Category;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.common.PageResponse;
import com.carbontrack.common.RecordSource;
import com.carbontrack.common.Scope;
import com.carbontrack.company.Company;
import com.carbontrack.company.CompanyRepository;
import com.carbontrack.facility.Facility;
import com.carbontrack.facility.FacilityService;
import com.carbontrack.factor.EmissionFactor;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    private static final Long COMPANY = 42L;
    private static final Long USER = 7L;
    private static final int YEAR = Year.now().getValue() - 1;

    @Mock ActivityRecordRepository records;
    @Mock FacilityService facilities;
    @Mock EmissionCalculator calculator;
    @Mock CompanyRepository companies;

    private ActivityService service;
    private Facility facility;
    private EmissionFactor factor;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new ActivityService(records, facilities, calculator, companies);
        facility = new Facility();
        ReflectionTestUtils.setField(facility, "id", 3L);
        facility.setCompanyId(COMPANY);
        facility.setName("Pune Plant");
        factor = new EmissionFactor();
        ReflectionTestUtils.setField(factor, "id", 11L);
        factor.setValueKgCo2ePerUnit(new BigDecimal("0.710000"));
        factor.setSource("CEA");
        company = new Company();
    }

    private ActivityRequest req(Category c, String qty, int year, int month) {
        return new ActivityRequest(3L, c, new BigDecimal(qty), year, month);
    }

    private void stubCreateDependencies(Category c, String qty, int year, String co2e) {
        when(facilities.getOwned(COMPANY, 3L)).thenReturn(facility);
        when(calculator.calculate(eq(c), eq(new BigDecimal(qty)), eq(year)))
                .thenReturn(new CalculationResult(factor, new BigDecimal(co2e)));
        when(records.saveAndFlush(any(ActivityRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(companies.findById(COMPANY)).thenReturn(Optional.of(company));
    }

    @Test
    void createDerivesUnitAndScopeAndStoresFactorAndCo2e() {
        stubCreateDependencies(Category.ELECTRICITY, "1000", YEAR, "710.000");

        ActivityDto dto = service.create(COMPANY, USER, req(Category.ELECTRICITY, "1000", YEAR, 5));

        assertEquals("kWh", dto.unit());
        assertEquals(Scope.SCOPE_2, dto.scope());
        assertEquals(new BigDecimal("710.000"), dto.co2eKg());
        assertEquals(11L, dto.factorId());
        assertEquals(new BigDecimal("0.710000"), dto.factorValue());
        assertEquals("CEA", dto.factorSource());
        assertEquals(RecordSource.MANUAL, dto.source());
        assertEquals("Pune Plant", dto.facilityName());
    }

    @Test
    void createStampsCompanyAndCreator() {
        stubCreateDependencies(Category.DIESEL, "10", YEAR, "26.800");

        service.create(COMPANY, USER, req(Category.DIESEL, "10", YEAR, 1));

        org.mockito.ArgumentCaptor<ActivityRecord> c = org.mockito.ArgumentCaptor.forClass(ActivityRecord.class);
        verify(records).saveAndFlush(c.capture());
        assertEquals(COMPANY, c.getValue().getCompanyId());
        assertEquals(USER, c.getValue().getCreatedBy());
        assertEquals(Scope.SCOPE_1, c.getValue().getScope());
    }

    @Test
    void importPathStoresSourceCsvAndSkipsFacilityAndBaselineLookups() {
        when(calculator.calculate(eq(Category.ELECTRICITY), eq(new BigDecimal("1000")), eq(YEAR)))
                .thenReturn(new CalculationResult(factor, new BigDecimal("710.000")));
        when(records.saveAndFlush(any(ActivityRecord.class))).thenAnswer(i -> i.getArgument(0));

        ActivityDto dto = service.createForImport(COMPANY, USER, facility, req(Category.ELECTRICITY, "1000", YEAR, 5));

        assertEquals(RecordSource.CSV, dto.source());
        assertEquals(new BigDecimal("710.000"), dto.co2eKg());
        assertEquals("Pune Plant", dto.facilityName());
        verify(facilities, never()).getOwned(any(), any());
        verify(companies, never()).findById(any());
    }

    @Test
    void importPathStillRejectsAFutureYear() {
        assertThrows(BadRequestException.class, () -> service.createForImport(COMPANY, USER, facility,
                req(Category.DIESEL, "10", Year.now().getValue() + 1, 1)));
        verify(records, never()).saveAndFlush(any());
    }

    @Test
    void createSetsBaselineToEarliestYearWhenNull() {
        stubCreateDependencies(Category.DIESEL, "10", YEAR, "26.800");
        when(records.findMinYear(COMPANY)).thenReturn(YEAR - 2);

        service.create(COMPANY, USER, req(Category.DIESEL, "10", YEAR, 1));

        assertEquals(YEAR - 2, company.getBaselineYear());
    }

    @Test
    void createKeepsExistingBaseline() {
        company.setBaselineYear(2023);
        stubCreateDependencies(Category.DIESEL, "10", YEAR, "26.800");

        service.create(COMPANY, USER, req(Category.DIESEL, "10", YEAR, 1));

        assertEquals(2023, company.getBaselineYear());
        verify(records, never()).findMinYear(any());
    }

    @Test
    void rejectsFutureYearAndYearBefore2000() {
        assertThrows(BadRequestException.class,
                () -> service.create(COMPANY, USER, req(Category.DIESEL, "10", Year.now().getValue() + 1, 1)));
        assertThrows(BadRequestException.class,
                () -> service.create(COMPANY, USER, req(Category.DIESEL, "10", 1999, 1)));
        verify(records, never()).saveAndFlush(any());
    }

    @Test
    void rejectsFacilityOfAnotherCompany() {
        when(facilities.getOwned(COMPANY, 3L)).thenThrow(new NotFoundException("Facility not found"));

        assertThrows(NotFoundException.class,
                () -> service.create(COMPANY, USER, req(Category.DIESEL, "10", YEAR, 1)));
        verify(records, never()).saveAndFlush(any());
    }

    @Test
    void updateRecalculatesAndKeepsSource() {
        ActivityRecord existing = new ActivityRecord();
        existing.setCompanyId(COMPANY);
        existing.setFacility(facility);
        existing.setFactor(factor);
        existing.setSource(RecordSource.SEED);
        existing.setCo2eKg(new BigDecimal("1.000"));
        when(records.findByIdAndCompanyId(9L, COMPANY)).thenReturn(Optional.of(existing));
        stubCreateDependencies(Category.PETROL, "20", YEAR, "46.200");

        ActivityDto dto = service.update(COMPANY, 9L, req(Category.PETROL, "20", YEAR, 2));

        assertEquals(new BigDecimal("46.200"), dto.co2eKg());
        assertEquals(Category.PETROL, dto.category());
        assertEquals("litre", dto.unit());
        assertEquals(RecordSource.SEED, dto.source());
    }

    @Test
    void updateAndDeleteOfAnotherCompanysRecordAreNotFound() {
        when(records.findByIdAndCompanyId(9L, COMPANY)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.update(COMPANY, 9L, req(Category.DIESEL, "10", YEAR, 1)));
        assertThrows(NotFoundException.class, () -> service.delete(COMPANY, 9L));
        verify(records, never()).delete(any(ActivityRecord.class));
    }

    @Test
    void listReturnsContractPagedShape() {
        ActivityRecord r = new ActivityRecord();
        r.setFacility(facility);
        r.setFactor(factor);
        r.setCategory(Category.DIESEL);
        Pageable p = PageRequest.of(1, 5);
        Page<ActivityRecord> page = new PageImpl<>(List.of(r), p, 11);
        when(records.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<ActivityDto> res = service.list(COMPANY, null, null, null, 1, 5);

        assertEquals(1, res.content().size());
        assertEquals(1, res.page());
        assertEquals(5, res.size());
        assertEquals(11, res.totalElements());
        assertEquals(3, res.totalPages());
    }

    @Test
    void listRejectsBadPaging() {
        assertThrows(BadRequestException.class, () -> service.list(COMPANY, null, null, null, -1, 20));
        assertThrows(BadRequestException.class, () -> service.list(COMPANY, null, null, null, 0, 0));
        assertThrows(BadRequestException.class, () -> service.list(COMPANY, null, null, null, 0, 101));
    }

    @Test
    void newCompanyHasNullBaselineByDefault() {
        assertNull(new Company().getBaselineYear());
    }
}
