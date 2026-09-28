package com.transgate.api.events;

import com.transgate.api.app.services.FiCreatedOutboxService;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retries PENDING FI-created outbox rows against RabbitMQ. Successful rows are deleted.
 */
@Component
public class FiCreatedOutboxPublisher {

    private static final Logger logger = Logger.getLogger(FiCreatedOutboxPublisher.class.getName());

    @Autowired
    @Qualifier("fiRabbitTemplate")
    private RabbitTemplate fiRabbitTemplate;

    @Autowired
    private FiCreatedOutboxService fiCreatedOutboxService;

    @Value("${app.rabbitmq.fi.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.fi.routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.fi.outbox-poll-batch-size:20}")
    private int batchSize;

    @Value("${app.rabbitmq.fi.outbox-max-attempts:20}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.rabbitmq.fi.outbox-poll-ms:60000}")
    public void publishPending() {
        int removedSent = fiCreatedOutboxService.deleteSentRows();
        if (removedSent > 0) {
            logger.info("Deleted " + removedSent + " already-sent FI created outbox row(s)");
        }
        List<FiCreatedOutboxService.PendingRow> pending = fiCreatedOutboxService.listPending(batchSize);
        for (FiCreatedOutboxService.PendingRow row : pending) {
            FinancialInstitutionCreatedMessage message = row.toMessage();
            try {
                fiRabbitTemplate.convertAndSend(exchangeName, routingKey, message);
                fiCreatedOutboxService.deletePublished(row.getId());
                logger.info("FI created outbox retry published and deleted for code=" + row.getCode());
            } catch (Exception e) {
                fiCreatedOutboxService.markPublishFailed(row.getId(), e.getMessage(), maxAttempts);
                logger.log(Level.WARNING,
                        "FI created outbox retry failed for code=" + row.getCode()
                                + " attempt=" + (row.getAttemptCount() + 1)
                                + " (will retry later unless max attempts reached): " + e.getMessage());
                return;
            }
        }
    }
}
