package com.hoardrun.auth.web;

import com.hoardrun.auth.domain.UserAccount;
import com.hoardrun.auth.repo.UserAccountRepository;
import com.hoardrun.common.security.JwtService;
import com.hoardrun.common.audit.AuditClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthController {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditClient auditClient;

    public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder, AuditClient auditClient) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        // For demo only; wire via @ConfigurationProperties in production
        this.jwtService = new JwtService("c2VjdXJlLXN1cGVyLXNlY3JldC1kZW1vLXNob3VsZC1iZS0zMi1ieXRlcy1vci1sb25nZXI=");
        this.auditClient = auditClient;
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 190) String username,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {}

    public record LoginRequest(
            @NotBlank @Size(min = 3, max = 190) String username,
            @NotBlank @Size(min = 8, max = 128) String password,
            @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(999999) Integer mfaCode
    ) {}

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (users.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username_taken"));
        }
        UserAccount ua = new UserAccount();
        ua.setUsername(req.username());
        ua.setPasswordHash(passwordEncoder.encode(req.password()));
        ua.setRole("USER");
        users.save(ua);
        auditClient.emit("AUTH_REGISTER", ua.getUsername(), null);
        return ResponseEntity.ok(Map.of("status", "registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        return users.findByUsername(req.username())
            .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
            .<ResponseEntity<?>>map(u -> {
                boolean mfaVerified = false;
                if (u.isMfaEnabled()) {
                    if (req.mfaCode() == null) {
                        return ResponseEntity.status(428).body(Map.of("error", "mfa_required"));
                    }
                    boolean verified = new com.hoardrun.auth.service.MfaService().verifyCode(u.getTotpSecret(), req.mfaCode());
                    if (!verified) return ResponseEntity.status(401).body(Map.of("error", "invalid_mfa"));
                    mfaVerified = true;
                }
                String access = jwtService.issueToken(u.getUsername(), 900, Map.of(
                        "role", u.getRole(),
                        "token_use", "access",
                        "mfa_verified", mfaVerified
                ));
                String refresh = jwtService.issueToken(u.getUsername(), 2592000, Map.of("token_use", "refresh"));
                auditClient.emit("AUTH_LOGIN", u.getUsername(), "{\"mfa\":" + mfaVerified + "}");
                return ResponseEntity.ok(Map.of("access_token", access, "refresh_token", refresh, "token_type", "Bearer"));
            })
            .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "invalid_credentials")));
    }
}


