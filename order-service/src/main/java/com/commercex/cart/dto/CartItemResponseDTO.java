package com.commercex.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Schema(description = "Cart line item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponseDTO {

    @Schema(description = "Cart item ID", example = "1")
    private Long id;
    @Schema(description = "Product ID", example = "7")
    private Long productId;
    @Schema(description = "Product name", example = "Wireless Headphones")
    private String productName;
    @Schema(description = "Unit price", example = "99.99")
    private BigDecimal unitPrice;
    @Schema(description = "Quantity in cart", example = "2")
    private Integer quantity;
    @Schema(description = "Line subtotal", example = "199.98")
    private BigDecimal subtotal;
}
