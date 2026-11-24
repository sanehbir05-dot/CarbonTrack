package com.carbontrack.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiry;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiry-minutes}") long expiryMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters (64+ recommended)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiry = Duration.ofMinutes(expiryMinutes);
    }

    public String generate(Long userId, Long companyId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("companyId", companyId)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry)))
                .signWith(key)
                .compact();
    }

    /** Returns the principal for a valid, unexpired token, or empty for anything else. */
    public Optional<AuthPrincipal> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Long companyId = c.get("companyId", Number.class).longValue();
            return Optional.of(new AuthPrincipal(Long.valueOf(c.getSubject()), companyId,
                    Role.valueOf(c.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
