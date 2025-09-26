package com.hoardrun.auth.web;

import com.hoardrun.common.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class TokenController {
    private final JwtService jwtService = new JwtService("c2VjdXJlLXN1cGVyLXNlY3JldC1kZW1vLXNob3VsZC1iZS0zMi1ieXRlcy1vci1sb25nZXI=");
    private final StringRedisTemplate redis;

    public TokenController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record RevokeRequest(@NotBlank String jti) {}

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        try {
            Jws<Claims> parsed = jwtService.parseAndValidate(req.refreshToken());
            Claims claims = parsed.getBody();
            if (!"refresh".equals(claims.get("token_use"))) {
                return ResponseEntity.status(400).body(Map.of("error", "invalid_token_use"));
            }
            String jti = claims.getId();
            if (jti != null && Boolean.TRUE.equals(redis.hasKey("bl:" + jti))) {
                return ResponseEntity.status(401).body(Map.of("error", "revoked"));
            }
            String sub = claims.getSubject();
            String access = jwtService.issueToken(sub, 900, Map.of("role", claims.get("role"), "token_use", "access"));
            return ResponseEntity.ok(Map.of("access_token", access, "token_type", "Bearer"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestBody RevokeRequest req) {
        redis.opsForValue().set("bl:" + req.jti(), "1", Duration.ofDays(30));
        return ResponseEntity.ok(Map.of("status", "revoked"));
    }
}


