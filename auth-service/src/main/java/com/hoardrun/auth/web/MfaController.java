package com.hoardrun.auth.web;

import com.hoardrun.auth.domain.UserAccount;
import com.hoardrun.auth.repo.UserAccountRepository;
import com.hoardrun.auth.service.MfaService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/mfa")
@Validated
public class MfaController {
    private final UserAccountRepository users;
    private final MfaService mfaService = new MfaService();

    public MfaController(UserAccountRepository users) {
        this.users = users;
    }

    public record SetupRequest(@NotBlank String username) {}
    public record VerifyRequest(@NotBlank String username, @NotNull Integer code) {}

    @PostMapping("/setup")
    @Transactional
    public ResponseEntity<?> setup(@RequestBody SetupRequest req) {
        UserAccount ua = users.findByUsername(req.username()).orElse(null);
        if (ua == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        String secret = mfaService.generateBase32Secret();
        ua.setTotpSecret(secret);
        users.save(ua);
        String uri = mfaService.provisioningUri(ua.getUsername(), "Hoardrun", secret);
        return ResponseEntity.ok(Map.of("secret", secret, "otpauth_uri", uri));
    }

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<?> verify(@RequestBody VerifyRequest req) {
        UserAccount ua = users.findByUsername(req.username()).orElse(null);
        if (ua == null || ua.getTotpSecret() == null) return ResponseEntity.status(400).body(Map.of("error", "invalid_state"));
        boolean ok = mfaService.verifyCode(ua.getTotpSecret(), req.code());
        if (!ok) return ResponseEntity.status(400).body(Map.of("error", "invalid_code"));
        ua.setMfaEnabled(true);
        users.save(ua);
        return ResponseEntity.ok(Map.of("status", "mfa_enabled"));
    }
}


