package com.transgate.api.events;

import com.transgate.api.app.services.FiCreatedOutboxService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Publishes FI-created jobs to the external RabbitMQ broker.
 */
@Component
public class FinancialInstitutionCreatedEventListener {

    private static final Logger logger = Logger.getLogger(FinancialInstitutionCreatedEventListener.class.getName());

    @Autowired
    @Qualifier("fiRabbitTemplate")
    private RabbitTemplate fiRabbitTemplate;

    @Autowired
    private FiCreatedOutboxService fiCreatedOutboxService;

    @Value("${app.rabbitmq.fi.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.fi.routing-key}")
    private String routingKey;

    @EventListener
    public void onFinancialInstitutionCreated(FinancialInstitutionCreatedEvent event) {
        if (event == null || event.getInstitution() == null) {
            logger.warning("FinancialInstitutionCreatedEvent missing institution; skip publish");
            return;
        }
        String code = event.getInstitution().getCode();
        Long outboxId = event.getOutboxId();
        FinancialInstitutionCreatedMessage message = FinancialInstitutionCreatedMessage.from(event.getInstitution());
        try {
            fiRabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            fiCreatedOutboxService.deletePublished(outboxId);
            logger.info("FI created message published to RabbitMQ for code=" + code);
        } catch (Exception e) {
            fiCreatedOutboxService.markPublishFailed(outboxId, e.getMessage());
            logger.log(Level.SEVERE,
                    "Failed to publish FI created message for code=" + code
                            + " (FI create still succeeded; outbox remains PENDING): " + e.getMessage(),
                    e);
        }
    }
}
