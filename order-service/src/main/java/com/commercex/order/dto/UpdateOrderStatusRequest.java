package com.commercex.order.dto;

import com.commercex.order.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "Update order status request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @Schema(description = "New order status", example = "SHIPPED")
    @NotNull(message = "Status is required")
    private OrderStatus status;
}
