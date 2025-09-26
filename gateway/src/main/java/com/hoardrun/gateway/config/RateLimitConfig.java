package com.hoardrun.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // Replenish 50 tokens per second, burst capacity 100
        return new RedisRateLimiter(50, 100);
    }
}


