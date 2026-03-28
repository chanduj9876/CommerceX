package com.commercex.order.event;

import com.commercex.common.ResourceNotFoundException;
import com.commercex.notification.service.NotificationService;
import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.repository.OrderRepository;
import com.commercex.shipping.service.ShipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for order status change events.
 *
 * Responsibilities:
 * - Logs every status change
 * - Auto-creates a shipment when order status → CONFIRMED
 * - Sends notifications on order confirmation via CompositeNotificationService
 *
 * Why @TransactionalEventListener(AFTER_COMMIT)?
 * The listener fires only after the transaction commits successfully.
 * This prevents side effects (emails, shipments) from running if the transaction
 * rolls back. Critical for notification reliability in production.
 */
@Slf4j
@Component
public class OrderEventListener {

    private final ShipmentService shipmentService;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderEventListener(
            ShipmentService shipmentService,
            OrderRepository orderRepository,
            @Qualifier("compositeNotification") NotificationService notificationService) {
        this.shipmentService = shipmentService;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.getOldStatus() == null) {
            log.info("[ORDER EVENT] Order {} created with status {}",
                    event.getOrderId(), event.getNewStatus());
        } else {
            log.info("[ORDER EVENT] Order {} status changed: {} → {}",
                    event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        }

        // Auto-create shipment + send notifications when order is confirmed
        if (event.getNewStatus() == OrderStatus.CONFIRMED) {
            handleOrderConfirmed(event.getOrderId());
        }
    }

    private void handleOrderConfirmed(Long orderId) {
        try {
            shipmentService.createShipment(orderId);
            log.info("[ORDER EVENT] Auto-created shipment for confirmed order {}", orderId);
        } catch (Exception e) {
            log.error("[ORDER EVENT] Failed to auto-create shipment for order {}: {}",
                    orderId, e.getMessage());
        }

        try {
            Order order = orderRepository.findByIdWithItems(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
            notificationService.sendOrderConfirmation(order.getUser(), order);
        } catch (Exception e) {
            log.error("[ORDER EVENT] Failed to send confirmation notifications for order {}: {}",
                    orderId, e.getMessage());
        }
    }
}
