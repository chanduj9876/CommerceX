package com.commercex.payment.gateway;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
public class CreditCardGatewayAdapter implements PaymentGateway {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public GatewayResponse processPayment(BigDecimal amount, String transactionId) {
        log.info("[CREDIT CARD] Processing payment of {} for transaction {}", amount, transactionId);
        simulateLatency();

        if (RANDOM.nextDouble() < 0.8) {
            String referenceId = "CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("[CREDIT CARD] Payment SUCCESS — ref: {}", referenceId);
            return GatewayResponse.success(referenceId);
        } else {
            log.warn("[CREDIT CARD] Payment DECLINED for transaction {}", transactionId);
            return GatewayResponse.failure("Card declined by issuer");
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(500 + RANDOM.nextInt(500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
