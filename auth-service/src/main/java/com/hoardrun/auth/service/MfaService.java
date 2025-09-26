package com.hoardrun.auth.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public class MfaService {
    private final TimeBasedOneTimePasswordGenerator totp;

    public MfaService() {
        try {
            this.totp = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize TOTP generator", e);
        }
    }

    public String generateBase32Secret() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(this.totp.getAlgorithm());
            keyGenerator.init(160);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot generate TOTP secret", e);
        }
    }

    public boolean verifyCode(String base64Secret, int code) {
        try {
            SecretKey key = new javax.crypto.spec.SecretKeySpec(Base64.getDecoder().decode(base64Secret), this.totp.getAlgorithm());
            int current = this.totp.generateOneTimePassword(key, Instant.now());
            return current == code;
        } catch (Exception e) {
            return false;
        }
    }

    public String provisioningUri(String account, String issuer, String base64Secret) {
        String secret = base64Secret;
        String label = URLEncoder.encode(issuer + ":" + account, StandardCharsets.UTF_8);
        String issuerEnc = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuerEnc + "&period=30&digits=6";
    }
}


