package com.commercex.shipping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "Manually create a shipment for an order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateShipmentRequest {

    @Schema(description = "Order ID to create shipment for", example = "42")
    @NotNull(message = "Order ID is required")
    private Long orderId;
}
