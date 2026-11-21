package com.carbontrack.common;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Identity of the caller. Services take companyId from here, never from the request. */
@Component
public class CurrentUser {

    public AuthPrincipal principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AuthPrincipal p)) {
            throw new AuthenticationCredentialsNotFoundException("Not authenticated");
        }
        return p;
    }

    public Long userId() {
        return principal().userId();
    }

    public Long companyId() {
        return principal().companyId();
    }

    public Role role() {
        return principal().role();
    }
}
