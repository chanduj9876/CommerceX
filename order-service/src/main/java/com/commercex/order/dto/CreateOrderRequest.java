package com.commercex.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Create order from cart (optionally with promo code)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @Schema(description = "Optional promo code for discount", example = "SAVE10")
    private String promoCode;
}
