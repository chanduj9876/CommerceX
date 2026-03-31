package com.commercex.payment.controller;

import com.commercex.payment.client.UserServiceClient;
import com.commercex.payment.dto.PaymentRequestDTO;
import com.commercex.payment.dto.PaymentResponseDTO;
import com.commercex.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payments", description = "Payment initiation and status tracking")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserServiceClient userServiceClient;

    @Operation(summary = "Initiate a payment for an order")
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponseDTO> initiatePayment(
            Authentication authentication,
            @Valid @RequestBody PaymentRequestDTO request) {
        Long userId = resolveUserId(authentication);
        PaymentResponseDTO response = paymentService.initiatePayment(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get payments for an order")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByOrder(
            Authentication authentication,
            @PathVariable Long orderId) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(orderId, userId));
    }

    @Operation(summary = "Confirm a payment by transaction ID (admin)")
    @PostMapping("/confirm/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponseDTO> confirmPayment(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(paymentService.confirmPayment(transactionId));
    }

    @Operation(summary = "Get payment by transaction ID")
    @GetMapping("/{transactionId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentByTransactionId(
            Authentication authentication,
            @PathVariable String transactionId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long userId = isAdmin ? null : resolveUserId(authentication);
        return ResponseEntity.ok(paymentService.getPaymentByTransactionId(transactionId, userId));
    }

    private Long resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userServiceClient.getUserByEmail(email).getId();
    }
}
