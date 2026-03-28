package com.commercex.cart.dto;

import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * DTO for adding an item to the cart.
 *
 * Why no cartId? The cart is derived from the authenticated user's JWT —
 * one cart per user, no need to specify which cart.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    private Long productId;

    @Positive(message = "Quantity must be at least 1")
    @jakarta.validation.constraints.Max(value = 999, message = "Quantity cannot exceed 999")
    private Integer quantity;
}
