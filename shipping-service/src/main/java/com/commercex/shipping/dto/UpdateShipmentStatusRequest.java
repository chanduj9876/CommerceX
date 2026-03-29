package com.commercex.shipping.dto;

import com.commercex.shipping.entity.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Schema(description = "Update shipment status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShipmentStatusRequest {

    @Schema(description = "New shipment status", example = "DELIVERED")
    @NotNull(message = "Status is required")
    private ShipmentStatus status;
}
