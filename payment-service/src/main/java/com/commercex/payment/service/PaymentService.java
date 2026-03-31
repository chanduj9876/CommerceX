package com.commercex.payment.service;

import com.commercex.payment.dto.PaymentRequestDTO;
import com.commercex.payment.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {

    PaymentResponseDTO initiatePayment(Long userId, PaymentRequestDTO request);

    List<PaymentResponseDTO> getPaymentsByOrderId(Long orderId, Long userId);

    PaymentResponseDTO getPaymentByTransactionId(String transactionId, Long userId);

    PaymentResponseDTO confirmPayment(String transactionId);
}
