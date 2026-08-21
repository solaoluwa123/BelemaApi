package com.transgate.api.events;

import com.transgate.api.util.SupersoftMailer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically drains the welcome-email DLQ and retries sendWelcomeMail.
 * On failure, re-publishes to the DLQ so messages are not lost.
 */
@Component
public class WelcomeEmailDlqPoller {

    private static final Logger logger = Logger.getLogger(WelcomeEmailDlqPoller.class.getName());

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SupersoftMailer supersoftMailer;

    @Value("${app.mail.welcome.dlq}")
    private String dlqName;

    @Value("${app.mail.welcome.dlq-poll-batch-size:20}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.mail.welcome.dlq-poll-ms:60000}")
    public void pollDlqAndRetry() {
        for (int i = 0; i < batchSize; i++) {
            Object raw = rabbitTemplate.receiveAndConvert(dlqName);
            if (raw == null) {
                return;
            }
            if (!(raw instanceof WelcomeEmailMessage)) {
                logger.warning("DLQ message was not WelcomeEmailMessage: " + raw.getClass().getName());
                continue;
            }
            WelcomeEmailMessage message = (WelcomeEmailMessage) raw;
            logger.info("[TEST] DLQ poller retrying welcome mail for " + message.getUserEmail());
            boolean sent = supersoftMailer.sendWelcomeMail(
                    message.getUserEmail(),
                    message.getUserName(),
                    message.getUserPassword(),
                    message.getFirstname(),
                    message.getSurname());
            if (!sent) {
                logger.log(Level.WARNING, "DLQ retry still failing for {0}; re-queueing to DLQ", message.getUserEmail());
                rabbitTemplate.convertAndSend(dlqName, message);
                // Stop this cycle to avoid tight fail loops when SMTP is down
                return;
            }
            logger.info("[TEST] DLQ poller successfully sent welcome mail to " + message.getUserEmail());
        }
    }
}
