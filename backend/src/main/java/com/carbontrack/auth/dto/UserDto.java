package com.carbontrack.auth.dto;

import com.carbontrack.auth.User;
import com.carbontrack.common.Role;

public record UserDto(Long id, String name, String email, Role role, Long companyId, String companyName,
                      Integer baselineYear) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCompany().getId(),
                u.getCompany().getName(), u.getCompany().getBaselineYear());
    }
}
