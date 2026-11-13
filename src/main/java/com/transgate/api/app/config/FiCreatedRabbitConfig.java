package com.transgate.api.app.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Single RabbitMQ connection (user {@code sw}) for FI-created, welcome-email and password-reset publishes.
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

    @Value("${app.mail.password-reset.exchange:belema.password.reset.exchange}")
    private String passwordResetExchangeName;

    @Value("${app.mail.password-reset.queue:belema.password.reset}")
    private String passwordResetQueueName;

    @Value("${app.mail.password-reset.routing-key:${app.mail.password-reset.queue:belema.password.reset}}")
    private String passwordResetRoutingKey;

    @Bean(name = {"connectionFactory", "fiConnectionFactory"})
    @Primary
    public CachingConnectionFactory fiConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.SIMPLE);
        return factory;
    }

    /** Runs the after-commit outbox relay. Rejected runs are dropped; the scheduled poller publishes those rows. */
    @Bean(name = "outboxRelayExecutor")
    public Executor outboxRelayExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("outbox-relay-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
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
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }

    /** Durable FI, welcome and password-reset topology; RabbitAdmin re-declares it on every new broker connection. */
    @Bean
    public Declarables belemaQueueTopology() {
        DirectExchange fiExchange = new DirectExchange(fiExchangeName, true, false);
        Queue fiQueue = QueueBuilder.durable(fiQueueName).build();
        DirectExchange welcomeExchange = new DirectExchange(welcomeExchangeName, true, false);
        Queue welcomeQueue = QueueBuilder.durable(welcomeQueueName).build();
        DirectExchange passwordResetExchange = new DirectExchange(passwordResetExchangeName, true, false);
        Queue passwordResetQueue = QueueBuilder.durable(passwordResetQueueName).build();
        return new Declarables(
                fiExchange,
                fiQueue,
                BindingBuilder.bind(fiQueue).to(fiExchange).with(fiRoutingKey),
                welcomeExchange,
                welcomeQueue,
                BindingBuilder.bind(welcomeQueue).to(welcomeExchange).with(welcomeRoutingKey),
                passwordResetExchange,
                passwordResetQueue,
                BindingBuilder.bind(passwordResetQueue).to(passwordResetExchange).with(passwordResetRoutingKey));
    }

    /** Declare the topology at startup; an unreachable broker is logged and does not block API startup. */
    @Bean
    public ApplicationRunner fiTopologyDeclarator(@Qualifier("fiRabbitAdmin") RabbitAdmin fiRabbitAdmin) {
        return args -> {
            try {
                fiRabbitAdmin.initialize();
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(FiCreatedRabbitConfig.class).warn(
                        "Could not declare RabbitMQ topology on {}:{} (publish may fail until broker is reachable): {}",
                        host, port, ex.getMessage());
            }
        };
    }
}
