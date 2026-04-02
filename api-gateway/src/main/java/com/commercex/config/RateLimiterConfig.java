package com.commercex.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Configuration for API Gateway rate limiting.
 * 
 * Defines how the rate limiter identifies unique clients.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Rate limit by IP address - each IP gets its own rate limit bucket.
     * 
     * Alternative strategies:
     * - By user (from JWT): return jwtUtil.getUsernameFromToken(token)
     * - By API key: return request.getHeaders().getFirst("X-API-Key")
     * - Global (all users share): return Mono.just("global")
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
            return Mono.just(clientIp);
        };
    }
}
