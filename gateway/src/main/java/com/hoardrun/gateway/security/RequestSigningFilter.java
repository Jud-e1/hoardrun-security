package com.hoardrun.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Simple HMAC-like request signing using shared secret and request path + timestamp.
 * Not for production without replay protection and body canonicalization.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestSigningFilter implements WebFilter {

    @Value("${security.signing.enabled:false}")
    private boolean signingEnabled;

    @Value("${security.signing.sharedSecret:}")
    private String sharedSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!signingEnabled) {
            return chain.filter(exchange);
        }
        String signature = exchange.getRequest().getHeaders().getFirst("X-Signature");
        String ts = exchange.getRequest().getHeaders().getFirst("X-Timestamp");
        if (signature == null || ts == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String payload = exchange.getRequest().getMethodValue() + "\n" + exchange.getRequest().getURI().getPath() + "\n" + ts + "\n" + sharedSecret;
        String expected = DigestUtils.appendMd5DigestAsHex(payload.getBytes(StandardCharsets.UTF_8), new StringBuilder()).toString();
        if (!expected.equalsIgnoreCase(signature)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}


