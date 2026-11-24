package com.carbontrack.action;

import com.carbontrack.action.dto.ActionDto;
import com.carbontrack.action.dto.ActionRequest;
import com.carbontrack.facility.Facility;
import com.carbontrack.facility.FacilityService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every method takes the caller's companyId. It never comes from the request body. */
@Service
public class ActionService {

    private final ReductionActionRepository actions;
    private final FacilityService facilities;

    public ActionService(ReductionActionRepository actions, FacilityService facilities) {
        this.actions = actions;
        this.facilities = facilities;
    }

    @Transactional(readOnly = true)
    public List<ActionDto> list(Long companyId) {
        return actions.findAllForCompany(companyId).stream().map(ActionDto::from).toList();
    }

    @Transactional
    public ActionDto create(Long companyId, ActionRequest req) {
        // A facility of another company is "not found" (404).
        Facility facility = req.facilityId() == null ? null : facilities.getOwned(companyId, req.facilityId());

        ReductionAction a = new ReductionAction();
        a.setCompanyId(companyId);
        a.setFacility(facility);
        a.setTitle(req.title().trim());
        a.setCategory(req.category());
        a.setExpectedSavingKg(req.expectedSavingKg());
        a.setStartDate(req.startDate());
        a.setStatus(req.status());
        return ActionDto.from(actions.save(a));
    }
}
