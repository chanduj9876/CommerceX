package com.commercex.shipping.service;

import com.commercex.common.ResourceNotFoundException;
import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.repository.OrderRepository;
import com.commercex.shipping.dto.ShipmentResponseDTO;
import com.commercex.shipping.entity.Shipment;
import com.commercex.shipping.entity.ShipmentStatus;
import com.commercex.shipping.event.ShipmentStatusChangedEvent;
import com.commercex.shipping.mapper.ShipmentMapper;
import com.commercex.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Valid shipment status transitions stored in an EnumMap for O(1) lookup.
     * EnumMap is more memory-efficient than HashMap for enum keys (uses a simple array internally).
     */
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(ShipmentStatus.class);
        VALID_TRANSITIONS.put(ShipmentStatus.PROCESSING, Set.of(ShipmentStatus.SHIPPED));
        VALID_TRANSITIONS.put(ShipmentStatus.SHIPPED, Set.of(ShipmentStatus.IN_TRANSIT));
        VALID_TRANSITIONS.put(ShipmentStatus.IN_TRANSIT, Set.of(ShipmentStatus.DELIVERED));
        VALID_TRANSITIONS.put(ShipmentStatus.DELIVERED, Set.of());
    }

    @Override
    @Transactional
    @CacheEvict(value = "shipments", allEntries = true)
    public ShipmentResponseDTO createShipment(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Cannot create shipment for order with status: " + order.getStatus()
                            + ". Order must be CONFIRMED.");
        }

        // Prevent duplicate shipments
        shipmentRepository.findByOrderId(orderId).ifPresent(existing -> {
            throw new IllegalStateException("Shipment already exists for order: " + orderId);
        });

        Shipment shipment = Shipment.builder()
                .order(order)
                .trackingId(UUID.randomUUID().toString())
                .status(ShipmentStatus.PROCESSING)
                .estimatedDelivery(LocalDate.now().plusDays(7))
                .build();

        shipment = shipmentRepository.save(shipment);
        log.info("[SHIPMENT] Created shipment {} for order {} with tracking ID {}",
                shipment.getId(), orderId, shipment.getTrackingId());

        return ShipmentMapper.toResponseDTO(shipment);
    }

    @Override
    @Transactional
    @CacheEvict(value = "shipments", allEntries = true)
    public ShipmentResponseDTO updateStatus(String trackingId, ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with tracking ID: " + trackingId));

        ShipmentStatus oldStatus = shipment.getStatus();
        validateStatusTransition(oldStatus, newStatus);

        shipment.setStatus(newStatus);
        shipment = shipmentRepository.save(shipment);

        log.info("[SHIPMENT] Shipment {} status changed: {} → {}", trackingId, oldStatus, newStatus);

        eventPublisher.publishEvent(new ShipmentStatusChangedEvent(
                shipment.getId(), shipment.getOrder().getId(), trackingId, oldStatus, newStatus));

        return ShipmentMapper.toResponseDTO(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "shipments", key = "#trackingId")
    public ShipmentResponseDTO trackShipment(String trackingId) {
        log.info("[CACHE MISS] Shipment tracking {} not in cache — loading from DB", trackingId);
        Shipment shipment = shipmentRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with tracking ID: " + trackingId));
        return ShipmentMapper.toResponseDTO(shipment);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "shipments", key = "'order-' + #orderId")
    public ShipmentResponseDTO getShipmentByOrderId(Long orderId) {
        log.info("[CACHE MISS] Shipment for order {} not in cache — loading from DB", orderId);
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order: " + orderId));
        return ShipmentMapper.toResponseDTO(shipment);
    }

    private void validateStatusTransition(ShipmentStatus current, ShipmentStatus next) {
        Set<ShipmentStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new IllegalStateException(
                    "Invalid shipment status transition: " + current + " → " + next);
        }
    }
}
