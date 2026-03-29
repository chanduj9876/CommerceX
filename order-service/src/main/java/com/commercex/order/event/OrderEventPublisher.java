package com.commercex.order.event;

import com.commercex.order.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishOrderCreated(Long orderId, OrderStatus status) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, null, status));
    }

    public void publishStatusChanged(Long orderId, OrderStatus oldStatus, OrderStatus newStatus) {
        eventPublisher.publishEvent(new OrderStatusChangedEvent(orderId, oldStatus, newStatus));
    }
}
