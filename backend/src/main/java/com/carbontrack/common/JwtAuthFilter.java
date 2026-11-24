package com.carbontrack.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer ..." and, for a valid token, puts an AuthPrincipal in the security context.
 * An invalid token is ignored, so the entry point answers 401. This is not a Spring bean on purpose:
 * it is created inside the security chain so it does not also run as a plain servlet filter.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.parse(header.substring(7).trim()).ifPresent(p -> {
                var auth = new UsernamePasswordAuthenticationToken(p, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + p.role().name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
