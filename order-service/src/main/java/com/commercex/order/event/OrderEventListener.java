package com.commercex.order.event;

import com.commercex.config.RabbitMQConfig;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Listens for order status change events (local Spring events).
 * On CONFIRMED, publishes to RabbitMQ for shipping-service and notification-service
 * to handle asynchronously (instead of direct ShipmentService/NotificationService calls).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.getOldStatus() == null) {
            log.info("[ORDER EVENT] Order {} created with status {}",
                    event.getOrderId(), event.getNewStatus());
        } else {
            log.info("[ORDER EVENT] Order {} status changed: {} → {}",
                    event.getOrderId(), event.getOldStatus(), event.getNewStatus());
        }

        if (event.getNewStatus() == OrderStatus.CONFIRMED) {
            handleOrderConfirmed(event.getOrderId());
        }
    }

    private void handleOrderConfirmed(Long orderId) {
        try {
            Map<String, Object> message = Map.of("orderId", orderId);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    "order.confirmed",
                    message);
            log.info("[ORDER EVENT] Published order.confirmed event for order {}", orderId);
        } catch (Exception e) {
            log.error("[ORDER EVENT] Failed to publish order.confirmed event for order {}: {}",
                    orderId, e.getMessage());
        }
    }
}
