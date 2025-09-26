package com.hoardrun.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods = false)
public class KeyResolvers {
    @Bean
    public KeyResolver remoteAddressResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress())
            .map(addr -> addr == null ? "unknown" : addr.getAddress().getHostAddress());
    }
}


