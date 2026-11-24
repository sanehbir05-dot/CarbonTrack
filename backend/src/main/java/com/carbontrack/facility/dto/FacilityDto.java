package com.carbontrack.facility.dto;

import com.carbontrack.common.FacilityType;
import com.carbontrack.facility.Facility;

public record FacilityDto(Long id, String name, String city, String state, FacilityType type) {

    public static FacilityDto from(Facility f) {
        return new FacilityDto(f.getId(), f.getName(), f.getCity(), f.getState(), f.getType());
    }
}
