package com.commercex.order.event;

import com.commercex.order.entity.Order;
import com.commercex.order.entity.OrderStatus;
import com.commercex.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Consumes payment.completed events from RabbitMQ (published by payment-service).
 * Updates order status from PENDING → CONFIRMED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    @RabbitListener(queues = "payment.completed.queue")
    @Transactional
    public void handlePaymentCompleted(Map<String, Object> event) {
        Long orderId = ((Number) event.get("orderId")).longValue();
        String transactionId = (String) event.get("transactionId");

        log.info("[PAYMENT EVENT] Received PaymentCompletedEvent — order: {}, txn: {}",
                orderId, transactionId);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.error("[PAYMENT EVENT] Order {} not found — ignoring event", orderId);
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
                order.getId(), transactionId);
    }
}
