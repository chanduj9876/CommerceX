package com.commercex.payment.dto;

import com.commercex.payment.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "Initiate a payment for an order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDTO {

    @Schema(description = "Order ID to pay", example = "42")
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Schema(description = "Payment method", example = "CREDIT_CARD")
    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
}
