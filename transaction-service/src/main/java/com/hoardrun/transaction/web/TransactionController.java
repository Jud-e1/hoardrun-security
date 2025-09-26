package com.hoardrun.transaction.web;

import org.springframework.security.access.prepost.PreAuthorize;
import com.hoardrun.common.audit.AuditClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tx")
public class TransactionController {
    private final AuditClient auditClient;

    public TransactionController(AuditClient auditClient) {
        this.auditClient = auditClient;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('SCOPE_transactions:read') or hasRole('USER')")
    public Map<String, Object> status() {
        return Map.of("status", "ok");
    }

    public record TxRequest(@jakarta.validation.constraints.Positive(message = "amount must be positive") double amount) {}

    @PostMapping("/submit")
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> submit(@jakarta.validation.Valid @RequestBody TxRequest req, @AuthenticationPrincipal Jwt jwt) {
        boolean mfaVerified = Boolean.TRUE.equals(jwt.getClaim("mfa_verified"));
        if (req.amount() > 1000 && !mfaVerified) {
            auditClient.emit("TX_MFA_REQUIRED", jwt.getSubject(), "{\"amount\":" + req.amount() + "}");
            return Map.of("status", "mfa_required", "message", "2FA required for high-value transactions");
        }
        auditClient.emit("TX_ACCEPTED", jwt.getSubject(), "{\"amount\":" + req.amount() + "}");
        return Map.of("status", "accepted", "amount", req.amount());
    }
}


