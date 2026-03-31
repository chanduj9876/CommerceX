package com.commercex.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Schema(description = "Update cart item quantity")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartItemRequest {

    @Schema(description = "New quantity", example = "3")
    @Positive(message = "Quantity must be at least 1")
    @jakarta.validation.constraints.Max(value = 999, message = "Quantity cannot exceed 999")
    private Integer quantity;
}
