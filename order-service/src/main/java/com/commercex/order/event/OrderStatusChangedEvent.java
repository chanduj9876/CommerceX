package com.commercex.order.event;

import com.commercex.order.entity.OrderStatus;
import lombok.Getter;

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
