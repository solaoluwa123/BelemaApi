package com.transgate.api.app.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Second RabbitMQ connection for financial-institution-created events.
 * Topology is declared only on this broker (not the welcome-mail Belema broker).
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
    private String exchangeName;

    @Value("${app.rabbitmq.fi.queue}")
    private String queueName;

    @Value("${app.rabbitmq.fi.routing-key}")
    private String routingKey;

    @Bean(name = "fiConnectionFactory")
    public CachingConnectionFactory fiConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return factory;
    }

    @Bean(name = "fiRabbitTemplate")
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
     * Declare durable exchange/queue/binding on the FI broker only
     * (avoid registering AmqpAdmin beans that the default RabbitAdmin would declare on Belema).
     * Failures are logged so an unreachable FI broker does not block API startup.
     */
    @Bean
    public ApplicationRunner fiTopologyDeclarator(@Qualifier("fiRabbitAdmin") RabbitAdmin fiRabbitAdmin) {
        return args -> {
            try {
                DirectExchange exchange = new DirectExchange(exchangeName, true, false);
                Queue queue = QueueBuilder.durable(queueName).build();
                fiRabbitAdmin.declareExchange(exchange);
                fiRabbitAdmin.declareQueue(queue);
                fiRabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(routingKey));
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(FiCreatedRabbitConfig.class).warn(
                        "Could not declare FI RabbitMQ topology on {}:{} (FI publish may fail until broker is reachable): {}",
                        host, port, ex.getMessage());
            }
        };
    }
}
