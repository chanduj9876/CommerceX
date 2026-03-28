package com.commercex.payment.gateway;

import java.math.BigDecimal;

/**
 * Adapter pattern — abstracts payment gateway interactions.
 *
 * Why Adapter pattern? Each payment provider (credit card processor, UPI, wallet)
 * has a different API contract. The Adapter pattern normalizes them behind a single
 * interface so the PaymentService doesn't care which gateway is used.
 *
 * Adding a new payment provider (e.g., PayPal) only requires:
 * 1. Implement this interface
 * 2. Register it in PaymentGatewayFactory
 * No changes to PaymentService needed.
 */
public interface PaymentGateway {

    /**
     * Process a payment through this gateway.
     *
     * @param amount        the amount to charge
     * @param transactionId the unique transaction reference
     * @return the gateway result containing success/failure and optional reference
     */
    GatewayResponse processPayment(BigDecimal amount, String transactionId);
}
