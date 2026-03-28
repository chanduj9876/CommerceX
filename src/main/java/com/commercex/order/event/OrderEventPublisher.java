package com.commercex.order.event;

import com.commercex.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes order-related events via Spring's ApplicationEventPublisher.
 *
 * Why a separate publisher class (instead of publishing directly in OrderService)?
 * - Single Responsibility: OrderService handles business logic; this handles events
 * - Testability: Easy to mock in unit tests
 * - Extensibility: Can add logging, filtering, or batching here without touching OrderService
 */
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publishes an event when an order is first created.
     */
    public void publishOrderCreated(Long orderId, OrderStatus status) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, null, status));
    }

    /**
     * Publishes an event when an order's status changes.
     */
    public void publishStatusChanged(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, oldStatus, newStatus));
    }
}
