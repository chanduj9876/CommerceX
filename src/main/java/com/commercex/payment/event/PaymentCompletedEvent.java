package com.commercex.payment.event;

import lombok.*;

import java.io.Serializable;

/**
 * Event published when a payment succeeds.
 * Consumed by PaymentEventConsumer to update order status to CONFIRMED.
 *
 * Why Serializable + no-args constructor? Required for Jackson JSON serialization
 * when sending/receiving via RabbitMQ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent implements Serializable {

    private Long orderId;
    private String transactionId;
}
