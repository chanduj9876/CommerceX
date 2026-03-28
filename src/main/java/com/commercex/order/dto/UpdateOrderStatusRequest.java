package com.commercex.order.dto;

import com.commercex.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for updating an order's status (admin only).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}
