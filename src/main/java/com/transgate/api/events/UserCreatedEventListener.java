package com.transgate.api.events;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Publishes welcome-email jobs to RabbitMQ when a {@link UserCreatedEvent} is raised.
 */
@Component
public class UserCreatedEventListener {

    private static final Logger logger = Logger.getLogger(UserCreatedEventListener.class.getName());

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.mail.welcome.exchange}")
    private String exchangeName;

    @Value("${app.mail.welcome.queue}")
    private String routingKey;

    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        // TEMP testing log — remove or redact password before production
        logger.info(String.format(
                "[TEST] UserCreatedEvent received: userEmail=%s, userName=%s, userPassword=%s, firstname=%s, surname=%s",
                event.getUserEmail(),
                event.getUserName(),
                event.getUserPassword(),
                event.getFirstname(),
                event.getSurname()));

        WelcomeEmailMessage message = WelcomeEmailMessage.from(event);
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            logger.info("[TEST] Welcome email message published to RabbitMQ for " + event.getUserEmail());
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Failed to publish welcome email message for " + event.getUserEmail()
                            + " (account create still succeeded): " + e.getMessage(),
                    e);
        }
    }
}
