package com.commercex.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Schema(description = "Individual order item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponseDTO {

    @Schema(description = "Line item ID", example = "1")
    private Long id;
    @Schema(description = "Product ID", example = "7")
    private Long productId;
    @Schema(description = "Product name", example = "Wireless Headphones")
    private String productName;
    @Schema(description = "Unit price at time of order", example = "99.99")
    private BigDecimal unitPrice;
    @Schema(description = "Quantity ordered", example = "2")
    private Integer quantity;
    @Schema(description = "Line subtotal", example = "199.98")
    private BigDecimal subtotal;
}
