package com.commercex.payment.gateway;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Simulated wallet payment gateway adapter.
 *
 * In production, this would integrate with a digital wallet provider.
 * Here it simulates a ~95% success rate (wallets are pre-funded, fewer failures).
 */
@Slf4j
public class WalletGatewayAdapter implements PaymentGateway {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public GatewayResponse processPayment(BigDecimal amount, String transactionId) {
        log.info("[WALLET] Processing payment of {} for transaction {}", amount, transactionId);

        simulateLatency();

        // ~95% success rate
        if (RANDOM.nextDouble() < 0.95) {
            String referenceId = "WLT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("[WALLET] Payment SUCCESS — ref: {}", referenceId);
            return GatewayResponse.success(referenceId);
        } else {
            log.warn("[WALLET] Payment FAILED for transaction {}", transactionId);
            return GatewayResponse.failure("Insufficient wallet balance");
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(200 + RANDOM.nextInt(300));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
