package com.carbontrack.dashboard;

import com.carbontrack.common.CurrentUser;
import com.carbontrack.dashboard.dto.CategoryShare;
import com.carbontrack.dashboard.dto.DashboardSummary;
import com.carbontrack.dashboard.dto.FacilityShare;
import com.carbontrack.dashboard.dto.MonthlyPoint;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;
    private final CurrentUser currentUser;

    public DashboardController(DashboardService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/years")
    public List<Integer> years() {
        return service.years(currentUser.companyId());
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@RequestParam(required = false) Integer year) {
        return service.summary(currentUser.companyId(), year);
    }

    @GetMapping("/monthly")
    public List<MonthlyPoint> monthly(@RequestParam(required = false) Integer year,
                                      @RequestParam(required = false) Long facilityId) {
        return service.monthly(currentUser.companyId(), year, facilityId);
    }

    @GetMapping("/by-category")
    public List<CategoryShare> byCategory(@RequestParam(required = false) Integer year) {
        return service.byCategory(currentUser.companyId(), year);
    }

    @GetMapping("/by-facility")
    public List<FacilityShare> byFacility(@RequestParam(required = false) Integer year) {
        return service.byFacility(currentUser.companyId(), year);
    }
}
