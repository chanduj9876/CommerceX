package com.commercex.payment.event;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration — declares exchange, queue, and binding.
 *
 * Architecture:
 *   PaymentEventProducer → [payment.exchange] → (routing key: payment.completed) → [payment.completed.queue] → PaymentEventConsumer
 *
 * Why Topic Exchange? Allows routing messages by pattern. Future payment events
 * (payment.refunded, payment.failed) can use the same exchange with different
 * routing keys and separate queues.
 *
 * Why JacksonJsonMessageConverter? Serializes Java objects to JSON for RabbitMQ
 * messages, making them human-readable and language-agnostic.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "payment.exchange";
    public static final String QUEUE_NAME = "payment.completed.queue";
    public static final String ROUTING_KEY = "payment.completed";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentCompletedQueue)
                .to(paymentExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
