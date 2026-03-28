package com.commercex.payment.entity;

/**
 * Payment lifecycle status.
 *
 * State transitions:
 *   PENDING → SUCCESS  (gateway confirms payment)
 *   PENDING → FAILED   (gateway rejects payment)
 *   SUCCESS → REFUNDED (admin-initiated refund)
 *
 * PENDING:  Payment initiated, awaiting gateway response
 * SUCCESS:  Gateway confirmed funds captured
 * FAILED:   Gateway rejected the transaction
 * REFUNDED: Payment was reversed after success
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
