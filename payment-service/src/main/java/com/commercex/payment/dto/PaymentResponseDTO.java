package com.commercex.payment.dto;

import com.commercex.payment.entity.PaymentMethod;
import com.commercex.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment details response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    @Schema(description = "Payment ID", example = "1")
    private Long id;
    @Schema(description = "Associated order ID", example = "42")
    private Long orderId;
    @Schema(description = "Amount charged", example = "179.98")
    private BigDecimal amount;
    @Schema(description = "Payment status", example = "COMPLETED")
    private PaymentStatus status;
    @Schema(description = "Payment method used", example = "CREDIT_CARD")
    private PaymentMethod method;
    @Schema(description = "Unique transaction ID", example = "TXN-abc123")
    private String transactionId;
    @Schema(description = "Gateway reference ID")
    private String referenceId;
    @Schema(description = "Failure reason if payment failed")
    private String failureReason;
    @Schema(description = "Payment creation timestamp")
    private LocalDateTime createdAt;
    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
