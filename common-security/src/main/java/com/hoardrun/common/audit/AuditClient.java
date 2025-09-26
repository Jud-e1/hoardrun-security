package com.hoardrun.common.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class AuditClient {
    private final RestClient restClient;

    public AuditClient(@Value("${audit.service.base-url:http://audit-service:8083}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void emit(String type, String principal, String detailsJson) {
        try {
            Map<String, Object> body = Map.of(
                "type", type,
                "principal", principal,
                "detailsJson", detailsJson
            );
            restClient.post().uri("/api/audit/ingest")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception ignored) {
            // Best-effort; do not break primary flow
        }
    }
}


