package com.commercex.cart.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for a single cart item in the response.
 * Includes product details so the client doesn't need to look them up separately.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponseDTO {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
