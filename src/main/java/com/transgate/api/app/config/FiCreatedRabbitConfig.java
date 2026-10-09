package com.transgate.api.app.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Single RabbitMQ connection (user {@code sw}) for FI-created and welcome-email publishes.
 */
@Configuration
public class FiCreatedRabbitConfig {

    @Value("${app.rabbitmq.fi.host}")
    private String host;

    @Value("${app.rabbitmq.fi.port}")
    private int port;

    @Value("${app.rabbitmq.fi.username}")
    private String username;

    @Value("${app.rabbitmq.fi.password}")
    private String password;

    @Value("${app.rabbitmq.fi.virtual-host}")
    private String virtualHost;

    @Value("${app.rabbitmq.fi.exchange}")
    private String fiExchangeName;

    @Value("${app.rabbitmq.fi.queue}")
    private String fiQueueName;

    @Value("${app.rabbitmq.fi.routing-key}")
    private String fiRoutingKey;

    @Value("${app.mail.welcome.exchange}")
    private String welcomeExchangeName;

    @Value("${app.mail.welcome.queue}")
    private String welcomeQueueName;

    @Value("${app.mail.welcome.routing-key:${app.mail.welcome.queue}}")
    private String welcomeRoutingKey;

    @Bean(name = {"connectionFactory", "fiConnectionFactory"})
    @Primary
    public CachingConnectionFactory fiConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return factory;
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean(name = "fiRabbitTemplate")
    @Primary
    public RabbitTemplate fiRabbitTemplate(
            @Qualifier("fiConnectionFactory") ConnectionFactory fiConnectionFactory,
            MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(fiConnectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }

    @Bean(name = "fiRabbitAdmin")
    public RabbitAdmin fiRabbitAdmin(@Qualifier("fiConnectionFactory") ConnectionFactory fiConnectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(fiConnectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    /**
     * Declare durable FI and welcome topology on the sw broker.
     * Failures are logged so an unreachable broker does not block API startup.
     */
    @Bean
    public ApplicationRunner fiTopologyDeclarator(@Qualifier("fiRabbitAdmin") RabbitAdmin fiRabbitAdmin) {
        return args -> {
            try {
                declareDirect(fiRabbitAdmin, fiExchangeName, fiQueueName, fiRoutingKey);
                declareDirect(fiRabbitAdmin, welcomeExchangeName, welcomeQueueName, welcomeRoutingKey);
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(FiCreatedRabbitConfig.class).warn(
                        "Could not declare RabbitMQ topology on {}:{} (publish may fail until broker is reachable): {}",
                        host, port, ex.getMessage());
            }
        };
    }

    private static void declareDirect(RabbitAdmin admin, String exchangeName, String queueName, String routingKey) {
        DirectExchange exchange = new DirectExchange(exchangeName, true, false);
        Queue queue = QueueBuilder.durable(queueName).build();
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routingKey));
    }
}
