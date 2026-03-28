package com.commercex.payment.entity;

/**
 * Supported payment methods.
 *
 * Each method maps to a specific PaymentGateway adapter:
 *   CREDIT_CARD → CreditCardGatewayAdapter
 *   UPI         → UpiGatewayAdapter
 *   WALLET      → WalletGatewayAdapter
 */
public enum PaymentMethod {
    CREDIT_CARD,
    UPI,
    WALLET
}
