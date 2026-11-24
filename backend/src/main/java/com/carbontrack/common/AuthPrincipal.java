package com.carbontrack.common;

/** The identity carried by a verified JWT. */
public record AuthPrincipal(Long userId, Long companyId, Role role) {
}
