package com.commercex.order.dto;

import lombok.*;

/**
 * DTO for creating an order from the cart.
 * PromoCode is optional — if provided, the discount strategy factory uses it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    private String promoCode;
}
