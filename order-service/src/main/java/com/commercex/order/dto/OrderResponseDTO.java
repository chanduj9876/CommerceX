package com.commercex.order.dto;

import com.commercex.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Order details response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    @Schema(description = "Order ID", example = "42")
    private Long id;
    @Schema(description = "Email of the ordering user", example = "jane@example.com")
    private String userEmail;
    @Schema(description = "Ordered items")
    private List<OrderItemResponseDTO> items;
    @Schema(description = "Subtotal before discount", example = "199.98")
    private BigDecimal subtotal;
    @Schema(description = "Discount applied", example = "20.00")
    private BigDecimal discountAmount;
    @Schema(description = "Promo code used", example = "SAVE10")
    private String promoCode;
    @Schema(description = "Final total after discount", example = "179.98")
    private BigDecimal totalAmount;
    @Schema(description = "Order status", example = "CONFIRMED")
    private OrderStatus status;
    @Schema(description = "Order creation timestamp")
    private LocalDateTime createdAt;
    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
