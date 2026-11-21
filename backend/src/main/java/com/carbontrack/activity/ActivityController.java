package com.carbontrack.activity;

import com.carbontrack.activity.dto.ActivityDto;
import com.carbontrack.activity.dto.ActivityRequest;
import com.carbontrack.common.Category;
import com.carbontrack.common.CurrentUser;
import com.carbontrack.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService service;
    private final CurrentUser currentUser;

    public ActivityController(ActivityService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<ActivityDto> list(@RequestParam(required = false) Long facilityId,
                                          @RequestParam(required = false) Category category,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return service.list(currentUser.companyId(), facilityId, category, year, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityDto create(@Valid @RequestBody ActivityRequest req) {
        return service.create(currentUser.companyId(), currentUser.userId(), req);
    }

    @PutMapping("/{id}")
    public ActivityDto update(@PathVariable Long id, @Valid @RequestBody ActivityRequest req) {
        return service.update(currentUser.companyId(), id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(currentUser.companyId(), id);
    }
}
