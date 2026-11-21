package com.carbontrack.incentive;

import com.carbontrack.common.CurrentUser;
import com.carbontrack.incentive.dto.IncentiveRuleDto;
import com.carbontrack.incentive.dto.IncentiveStatus;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incentives")
public class IncentiveController {

    private final IncentiveService service;
    private final CurrentUser currentUser;

    public IncentiveController(IncentiveService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/rules")
    public List<IncentiveRuleDto> rules() {
        return service.rules();
    }

    @GetMapping("/status")
    public IncentiveStatus status(@RequestParam(required = false) Integer year) {
        return service.status(currentUser.companyId(), year);
    }
}
