package com.carbontrack.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbontrack.common.BadRequestException;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.common.TargetStatus;
import com.carbontrack.dashboard.DashboardService;
import com.carbontrack.target.dto.TargetDto;
import com.carbontrack.target.dto.TargetRequest;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TargetServiceTest {

    private static final Long C = 42L;
    private static final int REPORTING_YEAR = Year.now().getValue() - 1;

    @Mock TargetRepository repo;
    @Mock DashboardService dashboard;
    @InjectMocks TargetService service;

    private static Target target(long id, int baseline, int targetYear, String pct) {
        Target t = new Target();
        ReflectionTestUtils.setField(t, "id", id);
        t.setCompanyId(C);
        t.setName("T" + id);
        t.setBaselineYear(baseline);
        t.setTargetYear(targetYear);
        t.setReductionPct(new BigDecimal(pct));
        return t;
    }

    @Test
    void listComputesProgressOnReadForTheReportingYear() {
        when(repo.findByCompanyIdOrderByIdAsc(C)).thenReturn(List.of(target(1, 2023, REPORTING_YEAR + 2, "20")));
        when(dashboard.yearTotal(C, 2023)).thenReturn(new BigDecimal("1000.000"));
        when(dashboard.yearTotal(C, REPORTING_YEAR)).thenReturn(new BigDecimal("500.000"));

        TargetDto dto = service.list(C).get(0);

        assertEquals(REPORTING_YEAR, dto.currentYear());
        assertEquals(new BigDecimal("800.000"), dto.targetKg());
        assertEquals(TargetStatus.ACHIEVED, dto.status());
    }

    @Test
    void listLooksUpEachBaselineYearOnlyOnce() {
        when(repo.findByCompanyIdOrderByIdAsc(C)).thenReturn(List.of(
                target(1, 2023, 2027, "20"), target(2, 2023, 2028, "30"), target(3, 2024, 2028, "10")));
        when(dashboard.yearTotal(any(), anyInt())).thenReturn(new BigDecimal("1000.000"));

        service.list(C);

        verify(dashboard, times(1)).yearTotal(C, 2023);
        verify(dashboard, times(1)).yearTotal(C, 2024);
        verify(dashboard, times(1)).yearTotal(C, REPORTING_YEAR);
    }

    @Test
    void createStampsCompanyFromCallerAndTrimsName() {
        when(dashboard.yearTotal(any(), anyInt())).thenReturn(BigDecimal.ZERO.setScale(3));

        TargetDto dto = service.create(C, new TargetRequest("  20% by 2027 ", 2023, 2027, new BigDecimal("20")));

        ArgumentCaptor<Target> c = ArgumentCaptor.forClass(Target.class);
        verify(repo).save(c.capture());
        assertEquals(C, c.getValue().getCompanyId());
        assertEquals("20% by 2027", c.getValue().getName());
        assertEquals(TargetStatus.AT_RISK, dto.status());   // no data yet: zero baseline
    }

    @Test
    void createRejectsTargetYearNotAfterBaseline() {
        assertThrows(BadRequestException.class,
                () -> service.create(C, new TargetRequest("x", 2025, 2025, new BigDecimal("10"))));
        assertThrows(BadRequestException.class,
                () -> service.create(C, new TargetRequest("x", 2025, 2024, new BigDecimal("10"))));
        verify(repo, never()).save(any());
    }

    @Test
    void createRejectsBaselineInTheFuture() {
        int future = Year.now().getValue() + 1;

        assertThrows(BadRequestException.class,
                () -> service.create(C, new TargetRequest("x", future, future + 3, new BigDecimal("10"))));
    }

    @Test
    void deleteOfAnotherCompanysTargetIsNotFound() {
        when(repo.findByIdAndCompanyId(9L, C)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(C, 9L));
        verify(repo, never()).delete(any(Target.class));
    }

    @Test
    void deleteRemovesOwnTarget() {
        Target t = target(9, 2023, 2027, "20");
        when(repo.findByIdAndCompanyId(9L, C)).thenReturn(Optional.of(t));

        service.delete(C, 9L);

        verify(repo).delete(t);
    }
}
