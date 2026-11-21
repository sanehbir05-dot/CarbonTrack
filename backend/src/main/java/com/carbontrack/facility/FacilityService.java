package com.carbontrack.facility;

import com.carbontrack.common.ConflictException;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.facility.dto.FacilityDto;
import com.carbontrack.facility.dto.FacilityRequest;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every method takes the companyId of the caller. It never comes from the request body. */
@Service
public class FacilityService {

    private final FacilityRepository facilities;

    public FacilityService(FacilityRepository facilities) {
        this.facilities = facilities;
    }

    @Transactional(readOnly = true)
    public List<FacilityDto> list(Long companyId) {
        return facilities.findByCompanyIdOrderByIdAsc(companyId).stream().map(FacilityDto::from).toList();
    }

    @Transactional
    public FacilityDto create(Long companyId, FacilityRequest req) {
        Facility f = new Facility();
        f.setCompanyId(companyId);
        apply(f, req);
        return FacilityDto.from(facilities.save(f));
    }

    @Transactional
    public FacilityDto update(Long companyId, Long id, FacilityRequest req) {
        Facility f = find(companyId, id);
        apply(f, req);
        return FacilityDto.from(f);
    }

    @Transactional
    public void delete(Long companyId, Long id) {
        Facility f = find(companyId, id);
        try {
            facilities.delete(f);
            facilities.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Facility has activity records and cannot be deleted");
        }
    }

    /** Used by other modules to confirm a facility belongs to the company. */
    @Transactional(readOnly = true)
    public Facility getOwned(Long companyId, Long id) {
        return find(companyId, id);
    }

    private Facility find(Long companyId, Long id) {
        return facilities.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Facility not found"));
    }

    private static void apply(Facility f, FacilityRequest req) {
        f.setName(req.name().trim());
        f.setCity(req.city());
        f.setState(req.state());
        f.setType(req.type());
    }
}
