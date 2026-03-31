package com.commercex.shipping.service;

import com.commercex.shipping.dto.ShipmentResponseDTO;
import com.commercex.shipping.entity.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShipmentService {

    ShipmentResponseDTO createShipment(Long orderId);

    ShipmentResponseDTO updateStatus(String trackingId, ShipmentStatus newStatus);

    ShipmentResponseDTO trackShipment(String trackingId);

    ShipmentResponseDTO getShipmentByOrderId(Long orderId);

    Page<ShipmentResponseDTO> getAllShipments(Pageable pageable);
}
