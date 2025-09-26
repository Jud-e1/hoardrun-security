package com.hoardrun.audit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
public class JwtConfig {
    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.secret}") String base64Secret) {
        byte[] key = Base64.getDecoder().decode(base64Secret);
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA512")).build();
    }
}


