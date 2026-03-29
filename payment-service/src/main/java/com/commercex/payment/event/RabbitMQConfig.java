package com.commercex.payment.event;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return new Jackson2JsonMessageConverter();
    }
}
