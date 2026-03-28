package com.commercex.order.dto;

import com.commercex.order.entity.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for the full order response.
 * Includes all items, totals, discount info, and status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {

    private Long id;
    private String userEmail;
    private List<OrderItemResponseDTO> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private String promoCode;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
