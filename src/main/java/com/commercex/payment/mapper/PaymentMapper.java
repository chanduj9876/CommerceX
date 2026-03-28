package com.commercex.payment.mapper;

import com.commercex.payment.dto.PaymentResponseDTO;
import com.commercex.payment.entity.Payment;

/**
 * Maps Payment entity to response DTO.
 */
public class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponseDTO toResponseDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .method(payment.getMethod())
                .transactionId(payment.getTransactionId())
                .referenceId(payment.getReferenceId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
