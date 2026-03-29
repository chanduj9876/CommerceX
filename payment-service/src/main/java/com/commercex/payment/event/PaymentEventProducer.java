package com.commercex.payment.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes payment events to RabbitMQ.
 * On successful payment, sends PaymentCompletedEvent to the payment exchange.
 * Order-service consumes it and updates order status to CONFIRMED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentCompleted(Long orderId, String transactionId) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(orderId)
                .transactionId(transactionId)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
        log.info("[PAYMENT EVENT] Published PaymentCompletedEvent via RabbitMQ — order: {}, txn: {}",
                orderId, transactionId);
    }
}
