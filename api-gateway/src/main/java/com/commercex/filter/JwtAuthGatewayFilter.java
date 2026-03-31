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
import java.util.Map;

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

    // Always-public regardless of HTTP method
    private static final List<String> PUBLIC_PATHS = List.of("/api/v1/auth/");

    // Public only for specific HTTP methods (method → path prefixes)
    private static final Map<String, List<String>> PUBLIC_METHOD_PATHS = Map.of(
            "GET", List.of("/api/v1/products", "/api/v1/categories")
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        if (isPublicPath(path, method)) {
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

    private boolean isPublicPath(String path, String method) {
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) return true;
        return PUBLIC_METHOD_PATHS.getOrDefault(method, List.of())
                .stream().anyMatch(path::startsWith);
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
