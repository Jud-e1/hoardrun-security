package com.hoardrun.audit.web;

import com.hoardrun.audit.domain.AuditEvent;
import com.hoardrun.audit.repo.AuditEventRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@Validated
public class AuditIngestController {
    private final AuditEventRepository repo;

    public AuditIngestController(AuditEventRepository repo) {
        this.repo = repo;
    }

    public record IngestRequest(@NotBlank String type, @NotBlank String principal, String detailsJson) {}

    @PostMapping("/ingest")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SCOPE_audit:write')")
    public ResponseEntity<?> ingest(@RequestBody IngestRequest req) {
        AuditEvent ev = new AuditEvent();
        ev.setType(req.type());
        ev.setPrincipal(req.principal());
        ev.setDetailsJson(req.detailsJson());
        repo.save(ev);
        return ResponseEntity.ok(Map.of("status", "ingested", "id", ev.getId()));
    }
}


