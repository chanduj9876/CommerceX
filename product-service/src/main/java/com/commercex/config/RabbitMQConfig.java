package com.commercex.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "commercex.events";

    @Bean
    public TopicExchange commercexExchange() {
        return new TopicExchange(EXCHANGE);
    }
}
