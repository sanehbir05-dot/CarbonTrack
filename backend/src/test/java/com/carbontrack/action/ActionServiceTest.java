package com.carbontrack.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbontrack.action.dto.ActionDto;
import com.carbontrack.action.dto.ActionRequest;
import com.carbontrack.common.ActionStatus;
import com.carbontrack.common.Category;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.facility.Facility;
import com.carbontrack.facility.FacilityService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

    private static final Long C = 42L;

    @Mock ReductionActionRepository repo;
    @Mock FacilityService facilities;
    @InjectMocks ActionService service;

    private static Facility facility() {
        Facility f = new Facility();
        ReflectionTestUtils.setField(f, "id", 3L);
        f.setName("Pune Plant");
        return f;
    }

    @Test
    void createStampsCompanyTrimsTitleAndChecksFacilityOwnership() {
        when(facilities.getOwned(C, 3L)).thenReturn(facility());
        when(repo.save(any(ReductionAction.class))).thenAnswer(i -> i.getArgument(0));

        ActionDto dto = service.create(C, new ActionRequest("  Solar roof ", 3L, Category.ELECTRICITY,
                new BigDecimal("12000"), LocalDate.of(2026, 1, 1), ActionStatus.PLANNED));

        ArgumentCaptor<ReductionAction> c = ArgumentCaptor.forClass(ReductionAction.class);
        verify(repo).save(c.capture());
        assertEquals(C, c.getValue().getCompanyId());
        assertEquals("Solar roof", c.getValue().getTitle());
        assertEquals(3L, dto.facilityId());
        assertEquals("Pune Plant", dto.facilityName());
        assertEquals(ActionStatus.PLANNED, dto.status());
    }

    @Test
    void companyWideActionNeedsNoFacility() {
        when(repo.save(any(ReductionAction.class))).thenAnswer(i -> i.getArgument(0));

        ActionDto dto = service.create(C, new ActionRequest("Green policy", null, null, null, null,
                ActionStatus.IN_PROGRESS));

        assertNull(dto.facilityId());
        assertNull(dto.facilityName());
        verify(facilities, never()).getOwned(any(), any());
    }

    @Test
    void facilityOfAnotherCompanyIsNotFound() {
        when(facilities.getOwned(C, 3L)).thenThrow(new NotFoundException("Facility not found"));

        assertThrows(NotFoundException.class, () -> service.create(C, new ActionRequest("x", 3L, null, null, null,
                ActionStatus.DONE)));
        verify(repo, never()).save(any());
    }

    @Test
    void listIsScopedToTheCompany() {
        ReductionAction a = new ReductionAction();
        a.setTitle("T");
        a.setStatus(ActionStatus.DONE);
        when(repo.findAllForCompany(C)).thenReturn(List.of(a));

        List<ActionDto> res = service.list(C);

        assertEquals(1, res.size());
        assertEquals("T", res.get(0).title());
        verify(repo).findAllForCompany(C);
    }
}
