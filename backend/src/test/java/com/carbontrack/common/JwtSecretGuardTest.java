package com.carbontrack.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class JwtSecretGuardTest {

    private static final String REAL = "a-real-secret-that-is-long-enough-and-not-the-default-0123456789abcdef";

    @Test
    void refusesTheDevSecretByDefault() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new JwtSecretGuard(JwtSecretGuard.DEV_SECRET, false));
        assertTrue(e.getMessage().contains("JWT_SECRET"));
        assertTrue(e.getMessage().contains("JWT_ALLOW_DEV_SECRET"));
    }

    @Test
    void allowsTheDevSecretWhenExplicitlyOptedIn() {
        assertDoesNotThrow(() -> new JwtSecretGuard(JwtSecretGuard.DEV_SECRET, true));
    }

    @Test
    void acceptsAnyOtherSecret() {
        assertDoesNotThrow(() -> new JwtSecretGuard(REAL, false));
        assertDoesNotThrow(() -> new JwtSecretGuard(REAL, true));
    }

    @Test
    void devSecretConstantMatchesTheFallbackInApplicationYml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        String raw = yaml.getObject().getProperty("app.jwt.secret");
        // raw looks like ${JWT_SECRET:<fallback>}
        assertEquals("${JWT_SECRET:" + JwtSecretGuard.DEV_SECRET + "}", raw);
    }

    @Test
    void defaultsToNotAllowingTheDevSecretInApplicationYml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        assertTrue(yaml.getObject().getProperty("app.jwt.allow-dev-secret").startsWith("${JWT_ALLOW_DEV_SECRET:false}"));
    }
}
