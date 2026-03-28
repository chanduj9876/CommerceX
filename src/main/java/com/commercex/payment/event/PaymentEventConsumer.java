package com.commercex.payment.event;

import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.event.OrderEventPublisher;
import com.commercex.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes payment events from RabbitMQ and updates order status.
 *
 * When a PaymentCompletedEvent arrives:
 * 1. Loads the order
 * 2. Verifies it's still in PENDING status (idempotency check)
 * 3. Updates status to CONFIRMED
 * 4. Publishes an OrderStatusChangedEvent (for logging, notifications, etc.)
 *
 * Why @RabbitListener instead of direct service call?
 * Loose coupling: the payment module and order module communicate through messages,
 * not direct method calls. This makes it easy to extract into separate microservices later.
 *
 * Why REQUIRES_NEW propagation? When RabbitMQ is unavailable, the fallback calls
 * this method directly from PaymentServiceImpl (inside an existing transaction).
 * REQUIRES_NEW ensures the order status update runs in its own transaction so a
 * failure here doesn't roll back the payment save.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("[PAYMENT EVENT] Received PaymentCompletedEvent — order: {}, txn: {}",
                event.getOrderId(), event.getTransactionId());

        Order order = orderRepository.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            log.error("[PAYMENT EVENT] Order {} not found — ignoring event", event.getOrderId());
            return;
        }

        // Idempotency: only transition PENDING → CONFIRMED
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("[PAYMENT EVENT] Order {} is already in {} status — skipping",
                    order.getId(), order.getStatus());
            return;
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        orderEventPublisher.publishStatusChanged(order.getId(), oldStatus, OrderStatus.CONFIRMED);

        log.info("[PAYMENT EVENT] Order {} confirmed via payment txn: {}",
                order.getId(), event.getTransactionId());
    }
}
