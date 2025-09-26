package com.hoardrun.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal JWT utility for issuing and validating HMAC-based tokens.
 * In production, prefer asymmetric keys (RSA/ECDSA) with rotation.
 */
public class JwtService {
    private final Key signingKey;

    public JwtService(String base64Secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }

    public String issueToken(String subject, long expiresSeconds, Map<String, Object> claims) {
        Instant now = Instant.now();
        Map<String, Object> mutable = new HashMap<>(claims == null ? Map.of() : claims);
        mutable.putIfAbsent("jti", UUID.randomUUID().toString());
        return Jwts.builder()
                .setClaims(mutable)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expiresSeconds)))
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Jws<Claims> parseAndValidate(String jwt) {
        return Jwts.parser().setSigningKey(signingKey).build().parseClaimsJws(jwt);
    }
}


