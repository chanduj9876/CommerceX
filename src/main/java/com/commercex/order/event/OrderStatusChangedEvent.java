package com.commercex.order.event;

import com.commercex.order.entity.OrderStatus;
import lombok.Getter;

/**
 * Event published when an order's status changes.
 *
 * Used by:
 * - OrderEventListener (logs the change)
 * - Future: ShipmentService (auto-create shipment on CONFIRMED)
 * - Future: NotificationService (email/SMS on status change)
 */
@Getter
public class OrderStatusChangedEvent {

    private final Long orderId;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangedEvent(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        this.orderId = orderId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
