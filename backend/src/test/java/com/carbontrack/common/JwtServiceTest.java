package com.carbontrack.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-unit-test-secret-0123456789";

    private final JwtService jwt = new JwtService(SECRET, 60);

    @Test
    void roundTripKeepsUserCompanyAndRole() {
        String token = jwt.generate(7L, 3L, Role.ANALYST);

        AuthPrincipal p = jwt.parse(token).orElseThrow();

        assertEquals(7L, p.userId());
        assertEquals(3L, p.companyId());
        assertEquals(Role.ANALYST, p.role());
    }

    @Test
    void garbageAndTamperedTokensAreRejected() {
        String token = jwt.generate(1L, 1L, Role.ADMIN);

        assertTrue(jwt.parse("not-a-token").isEmpty());
        assertTrue(jwt.parse(tamperSignature(token)).isEmpty());
    }

    /** Changes the first signature character. (The last one carries unused bits, so editing it can be a no-op.) */
    private static String tamperSignature(String token) {
        int sigStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(sigStart);
        char changed = first == 'A' ? 'B' : 'A';
        return token.substring(0, sigStart) + changed + token.substring(sigStart + 1);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService other = new JwtService("a-completely-different-secret-a-completely-different-secret-99", 60);

        assertTrue(jwt.parse(other.generate(1L, 1L, Role.ADMIN)).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expired = new JwtService(SECRET, -1);

        assertTrue(jwt.parse(expired.generate(1L, 1L, Role.ADMIN)).isEmpty());
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 60));
    }
}
