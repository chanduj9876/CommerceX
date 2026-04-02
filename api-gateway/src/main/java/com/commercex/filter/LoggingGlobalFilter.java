package com.commercex.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global logging filter for API Gateway.
 * 
 * Logs all incoming requestsand outgoing responses with timing information.
 * Runs at HIGHEST_PRECEDENCE + 1 to log before JWT validation.
 */
@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();
        String remoteAddress = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";

        log.info("→ Incoming request: {} {} from {}", method, path, remoteAddress);

        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            Integer statusCode = exchange.getResponse().getStatusCode() != null 
                    ? exchange.getResponse().getStatusCode().value() 
                    : null;
            
            log.info("← Response: {} {} → {} in {}ms", 
                    method, path, statusCode, duration);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;  // Run early, but after potential auth filters
    }
}
