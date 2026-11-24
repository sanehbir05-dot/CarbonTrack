package com.carbontrack.factor;

import com.carbontrack.factor.dto.FactorDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/factors")
public class FactorController {

    private final FactorService service;

    public FactorController(FactorService service) {
        this.service = service;
    }

    @GetMapping
    public List<FactorDto> list(@RequestParam(required = false) Integer year) {
        return service.list(year);
    }
}
