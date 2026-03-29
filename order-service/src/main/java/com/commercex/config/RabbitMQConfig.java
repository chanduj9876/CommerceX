package com.commercex.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "commercex.events";
    public static final String ORDER_CONFIRMED_QUEUE = "order.confirmed.queue";
    public static final String PRODUCT_DELETED_QUEUE = "product.deleted.queue";
    public static final String PAYMENT_COMPLETED_QUEUE = "payment.completed.queue";

    @Bean
    public TopicExchange commercexExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.exchange");
    }

    @Bean
    public Queue orderConfirmedQueue() {
        return new Queue(ORDER_CONFIRMED_QUEUE, true);
    }

    @Bean
    public Queue productDeletedQueue() {
        return new Queue(PRODUCT_DELETED_QUEUE, true);
    }

    @Bean
    public Queue paymentCompletedQueue() {
        return new Queue(PAYMENT_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding orderConfirmedBinding(Queue orderConfirmedQueue, TopicExchange commercexExchange) {
        return BindingBuilder.bind(orderConfirmedQueue).to(commercexExchange).with("order.confirmed");
    }

    @Bean
    public Binding productDeletedBinding(Queue productDeletedQueue, TopicExchange commercexExchange) {
        return BindingBuilder.bind(productDeletedQueue).to(commercexExchange).with("product.deleted");
    }

    @Bean
    public Binding paymentCompletedBinding(Queue paymentCompletedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentCompletedQueue).to(paymentExchange).with("payment.completed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
