package com.commercex.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;

/**
 * Global JWT authentication filter for the API Gateway.
 *
 * Runs before routing — validates Bearer tokens on all requests
 * except public paths (/api/v1/auth/**).
 * The filter is reactive (Mono<Void>) — compatible with Spring Cloud Gateway (WebFlux).
 * Does NOT use common-lib to avoid servlet/WebFlux classpath conflict.
 */
@Slf4j
@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[GATEWAY] Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            validateToken(token);
            return chain.filter(exchange);
        } catch (Exception e) {
            log.warn("[GATEWAY] JWT validation failed for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    @Override
    public int getOrder() {
        return -1; // Run before route filters
    }
}
