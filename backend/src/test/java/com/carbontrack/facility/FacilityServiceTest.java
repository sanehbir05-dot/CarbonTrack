package com.carbontrack.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbontrack.common.ConflictException;
import com.carbontrack.common.FacilityType;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.facility.dto.FacilityDto;
import com.carbontrack.facility.dto.FacilityRequest;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock FacilityRepository repo;
    @InjectMocks FacilityService service;

    private static final FacilityRequest REQ = new FacilityRequest(" Pune Plant ", "Pune", "MH", FacilityType.PLANT);

    @Test
    void createStampsCompanyFromCallerAndTrimsName() {
        when(repo.save(any(Facility.class))).thenAnswer(i -> i.getArgument(0));

        service.create(42L, REQ);

        ArgumentCaptor<Facility> c = ArgumentCaptor.forClass(Facility.class);
        verify(repo).save(c.capture());
        assertEquals(42L, c.getValue().getCompanyId());
        assertEquals("Pune Plant", c.getValue().getName());
    }

    @Test
    void otherCompaniesFacilityIsNotFound() {
        when(repo.findByIdAndCompanyId(5L, 42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(42L, 5L, REQ));
        assertThrows(NotFoundException.class, () -> service.delete(42L, 5L));
    }

    @Test
    void updateChangesFieldsOfOwnFacility() {
        Facility f = new Facility();
        ReflectionTestUtils.setField(f, "id", 5L);
        f.setCompanyId(42L);
        f.setName("Old");
        f.setType(FacilityType.OFFICE);
        when(repo.findByIdAndCompanyId(5L, 42L)).thenReturn(Optional.of(f));

        FacilityDto dto = service.update(42L, 5L, REQ);

        assertEquals("Pune Plant", dto.name());
        assertEquals(FacilityType.PLANT, dto.type());
    }

    @Test
    void deletingFacilityWithRecordsIsAConflict() {
        Facility f = new Facility();
        f.setCompanyId(42L);
        when(repo.findByIdAndCompanyId(5L, 42L)).thenReturn(Optional.of(f));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk")).when(repo).flush();

        assertThrows(ConflictException.class, () -> service.delete(42L, 5L));
    }
}
