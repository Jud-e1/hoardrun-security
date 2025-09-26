package com.hoardrun.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

class MfaServiceTest {

    private MfaService mfaService;

    @BeforeEach
    void setUp() {
        mfaService = new MfaService();
    }

    @Test
    void testGenerateBase32Secret() {
        String secret = mfaService.generateBase32Secret();
        assertNotNull(secret);
        // Base32 characters are A-Z and 2-7
        assertTrue(Pattern.matches("[A-Z2-7]+=*", secret));
    }

    @Test
    void testProvisioningUri() {
        String secret = mfaService.generateBase32Secret();
        String account = "testuser";
        String issuer = "TestApp";
        String uri = mfaService.provisioningUri(account, issuer, secret);

        assertNotNull(uri);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("issuer=" + issuer));
    }
}