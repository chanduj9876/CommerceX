package com.commercex.payment.dto;

import com.commercex.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for initiating a payment.
 * The amount comes from the order — only orderId and method are needed from the client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDTO {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
}
