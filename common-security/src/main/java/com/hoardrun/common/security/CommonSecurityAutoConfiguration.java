package com.hoardrun.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.hoardrun.common.audit.AuditClient;

/**
 * Auto-configuration providing common security beans shared across services.
 * - PasswordEncoder configured for NIST SP 800-63B using BCrypt.
 */
@Configuration(proxyBeanMethods = false)
public class CommonSecurityAutoConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with default strength 10 is acceptable for 2025 baseline; tune per CPU.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuditClient auditClient() {
        return new AuditClient(null);
    }
}


