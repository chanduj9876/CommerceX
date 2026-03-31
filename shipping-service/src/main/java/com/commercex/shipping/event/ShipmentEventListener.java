package com.commercex.shipping.event;

import com.commercex.common.ResourceNotFoundException;
import com.commercex.notification.service.NotificationService;
import com.commercex.shipping.client.OrderServiceClient;
import com.commercex.shipping.client.UserServiceClient;
import com.commercex.shipping.client.dto.OrderDTO;
import com.commercex.shipping.client.dto.UserDTO;
import com.commercex.shipping.entity.Shipment;
import com.commercex.shipping.entity.ShipmentStatus;
import com.commercex.shipping.repository.ShipmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
public class ShipmentEventListener {

    // Maps shipment status → the order status string to sync to order-service
    private static final Map<ShipmentStatus, String> ORDER_STATUS_SYNC = Map.of(
            ShipmentStatus.SHIPPED, "SHIPPED",
            ShipmentStatus.DELIVERED, "DELIVERED"
    );

    private final NotificationService notificationService;
    private final ShipmentRepository shipmentRepository;
    private final OrderServiceClient orderServiceClient;
    private final UserServiceClient userServiceClient;

    public ShipmentEventListener(
            @Qualifier("compositeNotification") NotificationService notificationService,
            ShipmentRepository shipmentRepository,
            OrderServiceClient orderServiceClient,
            UserServiceClient userServiceClient) {
        this.notificationService = notificationService;
        this.shipmentRepository = shipmentRepository;
        this.orderServiceClient = orderServiceClient;
        this.userServiceClient = userServiceClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentStatusChanged(ShipmentStatusChangedEvent event) {
        log.info("[SHIPMENT EVENT] Shipment {} (tracking: {}) status changed: {} → {}",
                event.getShipmentId(), event.getTrackingId(),
                event.getOldStatus(), event.getNewStatus());

        // Sync order status when shipment reaches SHIPPED or DELIVERED
        String orderStatus = ORDER_STATUS_SYNC.get(event.getNewStatus());
        if (orderStatus != null) {
            try {
                orderServiceClient.updateOrderStatus(event.getOrderId(), orderStatus);
                log.info("[SHIPMENT EVENT] Synced order {} status to {} (shipment {} is {})",
                        event.getOrderId(), orderStatus, event.getTrackingId(), event.getNewStatus());
            } catch (Exception e) {
                log.error("[SHIPMENT EVENT] Failed to sync order {} status to {}: {}",
                        event.getOrderId(), orderStatus, e.getMessage());
            }
        }

        // Send user notifications
        try {
            Shipment shipment = shipmentRepository.findById(event.getShipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Shipment not found: " + event.getShipmentId()));

            OrderDTO order = orderServiceClient.getOrder(event.getOrderId());
            UserDTO user = userServiceClient.getUser(order.getUserId());

            notificationService.sendShipmentUpdate(user, shipment);
        } catch (Exception e) {
            log.error("[SHIPMENT EVENT] Failed to send shipment notifications for tracking {}: {}",
                    event.getTrackingId(), e.getMessage());
        }
    }
}
