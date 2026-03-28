package com.commercex.payment.service;

import com.commercex.payment.dto.PaymentRequestDTO;
import com.commercex.payment.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {

    /**
     * Initiates a payment for the given order.
     * Creates a PENDING payment record, routes to the appropriate gateway adapter,
     * and updates status based on the gateway response.
     */
    PaymentResponseDTO initiatePayment(Long userId, PaymentRequestDTO request);

    /**
     * Retrieves all payment records for an order.
     */
    List<PaymentResponseDTO> getPaymentsByOrderId(Long orderId, Long userId);

    /**
     * Retrieves a specific payment by transaction ID.
     */
    PaymentResponseDTO getPaymentByTransactionId(String transactionId);

    /**
     * Confirms a PENDING payment by transaction ID.
     * Useful for async/webhook-style flows where the gateway confirms later.
     * On success, publishes PaymentCompletedEvent to update the order status.
     */
    PaymentResponseDTO confirmPayment(String transactionId);
}
