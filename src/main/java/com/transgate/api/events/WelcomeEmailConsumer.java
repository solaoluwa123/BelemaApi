package com.transgate.api.events;

import com.transgate.api.util.SupersoftMailer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Consumes welcome-email jobs from the main RabbitMQ queue.
 */
@Component
public class WelcomeEmailConsumer {

    private static final Logger logger = Logger.getLogger(WelcomeEmailConsumer.class.getName());

    @Autowired
    private SupersoftMailer supersoftMailer;

    @RabbitListener(queues = "${app.mail.welcome.queue}")
    public void onWelcomeEmail(WelcomeEmailMessage message) {
        logger.info("[TEST] WelcomeEmailConsumer received for " + message.getUserEmail());
        boolean sent = supersoftMailer.sendWelcomeMail(
                message.getUserEmail(),
                message.getUserName(),
                message.getUserPassword(),
                message.getFirstname(),
                message.getSurname());
        if (!sent) {
            logger.log(Level.SEVERE, "Welcome mail failed for {0}; routing to DLQ", message.getUserEmail());
            throw new IllegalStateException("Welcome mail send failed for " + message.getUserEmail());
        }
    }
}
