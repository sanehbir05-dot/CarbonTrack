package com.carbontrack.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carbontrack.auth.dto.AuthResponse;
import com.carbontrack.auth.dto.LoginRequest;
import com.carbontrack.auth.dto.RegisterRequest;
import com.carbontrack.common.ConflictException;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.Role;
import com.carbontrack.company.Company;
import com.carbontrack.company.CompanyRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock CompanyRepository companies;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt =
            new JwtService("unit-test-secret-unit-test-secret-unit-test-secret-0123456789", 60);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, companies, encoder, jwt);
    }

    @Test
    void registerCreatesAdminWithHashedPasswordAndNullBaseline() {
        when(users.existsByEmail("a@b.com")).thenReturn(false);
        when(companies.save(any(Company.class))).thenAnswer(i -> {
            Company c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 5L);
            return c;
        });
        when(users.saveAndFlush(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 9L);
            return u;
        });

        AuthResponse res = service.register(
                new RegisterRequest("Acme", "Steel", "Asha", "  A@B.com ", "Secret@123"));

        assertEquals(Role.ADMIN, res.user().role());
        assertEquals("a@b.com", res.user().email());
        assertEquals(5L, res.user().companyId());
        assertNull(res.user().baselineYear());
        assertEquals(5L, jwt.parse(res.token()).orElseThrow().companyId());
    }

    @Test
    void registerStoresBcryptHashNotPlaintext() {
        when(users.existsByEmail("a@b.com")).thenReturn(false);
        when(companies.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));
        when(users.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.register(new RegisterRequest("Acme", null, "Asha", "a@b.com", "Secret@123"));

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertNotEquals("Secret@123", captor.getValue().getPasswordHash());
        assertEquals(true, encoder.matches("Secret@123", captor.getValue().getPasswordHash()));
    }

    @Test
    void registerRejectsDuplicateEmailWith409() {
        when(users.existsByEmail("a@b.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.register(
                new RegisterRequest("Acme", null, "Asha", "a@b.com", "Secret@123")));
        verify(companies, never()).save(any());
    }

    @Test
    void loginRejectsWrongPassword() {
        User u = user("Secret@123");
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(u));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("a@b.com", "wrong")));
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(users.findByEmail("x@y.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("x@y.com", "whatever")));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user("Secret@123")));

        AuthResponse res = service.login(new LoginRequest("A@B.com", "Secret@123"));

        assertEquals(9L, jwt.parse(res.token()).orElseThrow().userId());
    }

    private User user(String rawPassword) {
        Company c = new Company();
        ReflectionTestUtils.setField(c, "id", 5L);
        c.setName("Acme");
        User u = new User();
        ReflectionTestUtils.setField(u, "id", 9L);
        u.setCompany(c);
        u.setName("Asha");
        u.setEmail("a@b.com");
        u.setRole(Role.ADMIN);
        u.setPasswordHash(encoder.encode(rawPassword));
        return u;
    }
}
