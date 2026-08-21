package com.transgate.api.app.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Welcome-email exchange, durable queue, and DLQ (idempotent if already declared on broker).
 */
@Configuration
public class WelcomeEmailRabbitConfig {

    @Value("${app.mail.welcome.exchange}")
    private String exchangeName;

    @Value("${app.mail.welcome.queue}")
    private String queueName;

    @Value("${app.mail.welcome.dlq}")
    private String dlqName;

    @Bean
    public DirectExchange welcomeEmailExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue welcomeEmailDlq() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Queue welcomeEmailQueue() {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange("")
                .deadLetterRoutingKey(dlqName)
                .build();
    }

    @Bean
    public Binding welcomeEmailBinding(Queue welcomeEmailQueue, DirectExchange welcomeEmailExchange) {
        return BindingBuilder.bind(welcomeEmailQueue).to(welcomeEmailExchange).with(queueName);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
