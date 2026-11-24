package com.carbontrack.facility;

import com.carbontrack.common.CurrentUser;
import com.carbontrack.facility.dto.FacilityDto;
import com.carbontrack.facility.dto.FacilityRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService service;
    private final CurrentUser currentUser;

    public FacilityController(FacilityService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<FacilityDto> list() {
        return service.list(currentUser.companyId());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public FacilityDto create(@Valid @RequestBody FacilityRequest req) {
        return service.create(currentUser.companyId(), req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public FacilityDto update(@PathVariable Long id, @Valid @RequestBody FacilityRequest req) {
        return service.update(currentUser.companyId(), id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(currentUser.companyId(), id);
    }
}
