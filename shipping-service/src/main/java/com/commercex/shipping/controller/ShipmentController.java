package com.commercex.shipping.controller;

import com.commercex.shipping.dto.CreateShipmentRequest;
import com.commercex.shipping.dto.ShipmentResponseDTO;
import com.commercex.shipping.dto.UpdateShipmentStatusRequest;
import com.commercex.shipping.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shipments", description = "Shipment tracking and status management")
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @Operation(summary = "Manually create a shipment (admin)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShipmentResponseDTO> createShipment(
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponseDTO response = shipmentService.createShipment(request.getOrderId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "List all shipments (admin)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ShipmentResponseDTO>> getAllShipments(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(shipmentService.getAllShipments(pageable));
    }

    @Operation(summary = "Track shipment by tracking ID")
    @GetMapping("/{trackingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShipmentResponseDTO> trackShipment(@PathVariable String trackingId) {
        return ResponseEntity.ok(shipmentService.trackShipment(trackingId));
    }

    @Operation(summary = "Update shipment status (admin)")
    @PatchMapping("/{trackingId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShipmentResponseDTO> updateShipmentStatus(
            @PathVariable String trackingId,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {
        return ResponseEntity.ok(shipmentService.updateStatus(trackingId, request.getStatus()));
    }

    @Operation(summary = "Get shipment by order ID")
    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ShipmentResponseDTO> getShipmentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentByOrderId(orderId));
    }
}
