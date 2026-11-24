package com.carbontrack.auth;

import com.carbontrack.auth.dto.AuthResponse;
import com.carbontrack.auth.dto.LoginRequest;
import com.carbontrack.auth.dto.RegisterRequest;
import com.carbontrack.auth.dto.UserDto;
import com.carbontrack.common.ConflictException;
import com.carbontrack.common.JwtService;
import com.carbontrack.common.NotFoundException;
import com.carbontrack.common.Role;
import com.carbontrack.company.Company;
import com.carbontrack.company.CompanyRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final CompanyRepository companies;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, CompanyRepository companies, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.companies = companies;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Creates the company and its first ADMIN user in one transaction. baselineYear starts null. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Company company = new Company();
        company.setName(req.companyName().trim());
        company.setIndustry(req.industry());
        companies.save(company);

        User user = new User();
        user.setCompany(company);
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(Role.ADMIN);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two registrations with the same email raced past existsByEmail.
            throw new ConflictException("An account with this email already exists");
        }
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email()))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return response(user);
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        return users.findWithCompanyById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private AuthResponse response(User user) {
        String token = jwt.generate(user.getId(), user.getCompany().getId(), user.getRole());
        return new AuthResponse(token, UserDto.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
