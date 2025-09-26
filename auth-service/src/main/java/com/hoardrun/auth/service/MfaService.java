package com.hoardrun.auth.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

public class MfaService {
    private final TimeBasedOneTimePasswordGenerator totp;
    private final Base32 base32;

    public MfaService() {
        this.totp = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30));
        this.base32 = new Base32();
    }

    public String generateBase32Secret() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(this.totp.getAlgorithm());
            keyGenerator.init(160);
            SecretKey key = keyGenerator.generateKey();
            return this.base32.encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot generate TOTP secret", e);
        }
    }

    public boolean verifyCode(String base32Secret, int code) {
        try {
            SecretKey key = new javax.crypto.spec.SecretKeySpec(this.base32.decode(base32Secret), this.totp.getAlgorithm());
            int current = this.totp.generateOneTimePassword(key, Instant.now());
            return current == code;
        } catch (Exception e) {
            return false;
        }
    }

    public String provisioningUri(String account, String issuer, String base32Secret) {
        String secret = base32Secret;
        String label = URLEncoder.encode(issuer + ":" + account, StandardCharsets.UTF_8);
        String issuerEnc = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + issuerEnc + "&period=30&digits=6";
    }
}


