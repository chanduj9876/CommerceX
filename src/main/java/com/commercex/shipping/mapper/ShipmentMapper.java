package com.commercex.shipping.mapper;

import com.commercex.shipping.dto.ShipmentResponseDTO;
import com.commercex.shipping.entity.Shipment;

public class ShipmentMapper {

    private ShipmentMapper() {}

    public static ShipmentResponseDTO toResponseDTO(Shipment shipment) {
        return ShipmentResponseDTO.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrder().getId())
                .trackingId(shipment.getTrackingId())
                .status(shipment.getStatus())
                .estimatedDelivery(shipment.getEstimatedDelivery())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}
