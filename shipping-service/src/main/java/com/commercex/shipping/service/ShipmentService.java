package com.commercex.shipping.service;

import com.commercex.shipping.dto.ShipmentResponseDTO;
import com.commercex.shipping.entity.ShipmentStatus;

public interface ShipmentService {

    ShipmentResponseDTO createShipment(Long orderId);

    ShipmentResponseDTO updateStatus(String trackingId, ShipmentStatus newStatus);

    ShipmentResponseDTO trackShipment(String trackingId);

    ShipmentResponseDTO getShipmentByOrderId(Long orderId);
}
