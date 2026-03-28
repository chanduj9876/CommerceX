package com.commercex.shipping.controller;

import com.commercex.shipping.dto.CreateShipmentRequest;
import com.commercex.shipping.dto.ShipmentResponseDTO;
import com.commercex.shipping.dto.UpdateShipmentStatusRequest;
import com.commercex.shipping.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShipmentResponseDTO> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponseDTO response = shipmentService.createShipment(request.getOrderId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{trackingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShipmentResponseDTO> trackShipment(@PathVariable String trackingId) {
        return ResponseEntity.ok(shipmentService.trackShipment(trackingId));
    }

    @PatchMapping("/{trackingId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShipmentResponseDTO> updateShipmentStatus(
            @PathVariable String trackingId,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {
        return ResponseEntity.ok(shipmentService.updateStatus(trackingId, request.getStatus()));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShipmentResponseDTO> getShipmentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentByOrderId(orderId));
    }
}
