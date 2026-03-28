package com.commercex.payment.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes payment events to RabbitMQ.
 *
 * On successful payment, sends a PaymentCompletedEvent to the payment exchange
 * with routing key "payment.completed". The PaymentEventConsumer picks it up
 * and updates the order status to CONFIRMED.
 *
 * If RabbitMQ is unavailable, falls back to direct in-memory invocation of the consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final PaymentEventConsumer paymentEventConsumer;

    public void publishPaymentCompleted(Long orderId, String transactionId) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(orderId)
                .transactionId(transactionId)
                .build();

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            log.info("[PAYMENT EVENT] Published PaymentCompletedEvent via RabbitMQ — order: {}, txn: {}",
                    orderId, transactionId);
        } catch (AmqpConnectException e) {
            log.warn("[PAYMENT EVENT] RabbitMQ unavailable, handling event directly — order: {}, txn: {}",
                    orderId, transactionId);
            paymentEventConsumer.handlePaymentCompleted(event);
        }
    }
}
