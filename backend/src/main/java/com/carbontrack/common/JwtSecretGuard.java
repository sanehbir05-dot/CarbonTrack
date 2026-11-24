package com.carbontrack.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stops the app from starting with the public development secret.
 *
 * <p>application.yml falls back to {@link #DEV_SECRET} so a missing JWT_SECRET does not break local setup,
 * but that string is in the repository, so anyone could forge tokens for a server that runs with it.
 * Outside local development the app must therefore be given its own JWT_SECRET. For local runs, either set
 * JWT_SECRET or set JWT_ALLOW_DEV_SECRET=true.
 */
@Component
public class JwtSecretGuard {

    /** Must stay identical to the fallback in application.yml (a test checks this). */
    static final String DEV_SECRET =
            "local-dev-only-secret-change-me-local-dev-only-secret-change-me-0123456789";

    public JwtSecretGuard(@Value("${app.jwt.secret}") String secret,
                          @Value("${app.jwt.allow-dev-secret:false}") boolean allowDevSecret) {
        check(secret, allowDevSecret);
    }

    static void check(String secret, boolean allowDevSecret) {
        if (DEV_SECRET.equals(secret) && !allowDevSecret) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set, so the public development secret would be used. "
                            + "Set JWT_SECRET to 64+ random characters. "
                            + "For local development only, set JWT_ALLOW_DEV_SECRET=true instead.");
        }
    }
}
