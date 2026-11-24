package com.carbontrack.facility.dto;

import com.carbontrack.common.FacilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FacilityRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @NotNull FacilityType type) {
}
