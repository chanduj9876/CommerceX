package com.commercex.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Cart contents response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponseDTO {

    @Schema(description = "Cart ID", example = "1")
    private Long id;
    @Schema(description = "Cart line items")
    private List<CartItemResponseDTO> items;
    @Schema(description = "Total number of items", example = "3")
    private int totalItems;
    @Schema(description = "Total price", example = "299.97")
    private BigDecimal totalPrice;
}
