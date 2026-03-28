package com.commercex.cart.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for the full cart response.
 * Includes all items with product details, quantities, and the cart total.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseDTO {

    private Long id;
    private List<CartItemResponseDTO> items;
    private int totalItems;
    private BigDecimal totalPrice;
}
