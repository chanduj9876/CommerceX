package com.commercex.shipping.dto;

import com.commercex.shipping.entity.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Shipment details response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentResponseDTO {

    @Schema(description = "Shipment ID", example = "1")
    private Long id;
    @Schema(description = "Order ID this shipment belongs to", example = "42")
    private Long orderId;
    @Schema(description = "Tracking ID", example = "TRK-20260329-abc123")
    private String trackingId;
    @Schema(description = "Current shipment status", example = "IN_TRANSIT")
    private ShipmentStatus status;
    @Schema(description = "Estimated delivery date", example = "2026-04-05")
    private LocalDate estimatedDelivery;
    @Schema(description = "Shipment creation timestamp")
    private LocalDateTime createdAt;
    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
