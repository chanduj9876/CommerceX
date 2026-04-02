package com.commercex.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Fallback controller for circuit breaker responses.
 * 
 * When a downstream service is unavailable or experiencing issues,
 * instead of failing completely, the circuit breaker redirects to these endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    @PostMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.warn("User service is currently unavailable - returning fallback response");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "User service is temporarily unavailable. Please try again later.",
                        "service", "user-service"
                ));
    }

    @GetMapping("/product-service")
    @PostMapping("/product-service")
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        log.warn("Product service is currently unavailable - returning fallback response");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Product service is temporarily unavailable. Please try again later.",
                        "service", "product-service"
                ));
    }

    @GetMapping("/order-service")
    @PostMapping("/order-service")
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        log.warn("Order service is currently unavailable - returning fallback response");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Order service is temporarily unavailable. Please try again later.",
                        "service", "order-service"
                ));
    }

    @GetMapping("/payment-service")
    @PostMapping("/payment-service")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.warn("Payment service is currently unavailable - returning fallback response");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Payment service is temporarily unavailable. Please try again later.",
                        "service", "payment-service"
                ));
    }

    @GetMapping("/shipping-service")
    @PostMapping("/shipping-service")
    public ResponseEntity<Map<String, Object>> shippingServiceFallback() {
        log.warn("Shipping service is currently unavailable - returning fallback response");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Shipping service is temporarily unavailable. Please try again later.",
                        "service", "shipping-service"
                ));
    }
}
