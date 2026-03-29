package com.commercex.payment.gateway;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
public class UpiGatewayAdapter implements PaymentGateway {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public GatewayResponse processPayment(BigDecimal amount, String transactionId) {
        log.info("[UPI] Processing payment of {} for transaction {}", amount, transactionId);
        simulateLatency();

        if (RANDOM.nextDouble() < 0.9) {
            String referenceId = "UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("[UPI] Payment SUCCESS — ref: {}", referenceId);
            return GatewayResponse.success(referenceId);
        } else {
            log.warn("[UPI] Payment FAILED for transaction {}", transactionId);
            return GatewayResponse.failure("UPI transaction timeout");
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(300 + RANDOM.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
