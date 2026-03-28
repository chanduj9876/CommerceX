package com.commercex.payment.controller;

import com.commercex.payment.dto.PaymentRequestDTO;
import com.commercex.payment.dto.PaymentResponseDTO;
import com.commercex.payment.service.PaymentService;
import com.commercex.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payment controller.
 *
 * Customer endpoints:
 *   POST  /api/v1/payments/initiate       — initiate payment for an order
 *   POST  /api/v1/payments/confirm/{transactionId} — confirm a PENDING payment (webhook/async)
 *   GET   /api/v1/payments/order/{orderId} — view payments for an order
 *   GET   /api/v1/payments/{transactionId} — view a specific payment by transaction ID
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponseDTO> initiatePayment(
            Authentication authentication,
            @Valid @RequestBody PaymentRequestDTO request) {
        Long userId = resolveUserId(authentication);
        PaymentResponseDTO response = paymentService.initiatePayment(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByOrder(
            Authentication authentication,
            @PathVariable Long orderId) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(orderId, userId));
    }

    @PostMapping("/confirm/{transactionId}")
    public ResponseEntity<PaymentResponseDTO> confirmPayment(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.confirmPayment(transactionId));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentByTransactionId(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.getPaymentByTransactionId(transactionId));
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.commercex.common.ResourceNotFoundException("User not found"))
                .getId();
    }
}
