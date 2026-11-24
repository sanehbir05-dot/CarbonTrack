package com.carbontrack.action;

import com.carbontrack.action.dto.ActionDto;
import com.carbontrack.action.dto.ActionRequest;
import com.carbontrack.common.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/actions")
public class ActionController {

    private final ActionService service;
    private final CurrentUser currentUser;

    public ActionController(ActionService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ActionDto> list() {
        return service.list(currentUser.companyId());
    }

    /** Any signed-in user can log an action. A7.4 leaves this open, and the team decided analysts may. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActionDto create(@Valid @RequestBody ActionRequest req) {
        return service.create(currentUser.companyId(), req);
    }
}
