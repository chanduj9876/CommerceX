package com.commercex.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Schema(description = "Add item to cart")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    @Schema(description = "Product ID to add", example = "7")
    private Long productId;

    @Schema(description = "Quantity to add", example = "2")
    @Positive(message = "Quantity must be at least 1")
    @jakarta.validation.constraints.Max(value = 999, message = "Quantity cannot exceed 999")
    private Integer quantity;
}
