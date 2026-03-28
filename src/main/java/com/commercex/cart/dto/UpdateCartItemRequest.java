package com.commercex.cart.dto;

import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * DTO for updating quantity of an existing cart item.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartItemRequest {

    @Positive(message = "Quantity must be at least 1")
    @jakarta.validation.constraints.Max(value = 999, message = "Quantity cannot exceed 999")
    private Integer quantity;
}
