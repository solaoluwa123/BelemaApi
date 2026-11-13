package com.transgate.api.events;

import java.util.logging.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publishes outbox rows to RabbitMQ. Each row is locked, published with a broker confirm and deleted in one
 * transaction, so a row is only removed once RabbitMQ has accepted the message.
 */
@Component
public class OutboxRelay {

    private static final Logger logger = Logger.getLogger(OutboxRelay.class.getName());

    @FunctionalInterface
    interface Sender {
        void send(String exchange, String routingKey, Object message, String messageId);
    }

    private enum Outcome {
        NOTHING_PENDING,
        PUBLISHED,
        FAILED
    }

    private final Sender sender;
    private final TransactionOperations transactions;

    @Autowired
    public OutboxRelay(@Qualifier("fiRabbitTemplate") RabbitTemplate rabbitTemplate,
            PlatformTransactionManager transactionManager,
            @Value("${app.outbox.confirm-timeout-ms:10000}") long confirmTimeoutMs) {
        this(confirmedSender(rabbitTemplate, confirmTimeoutMs), new TransactionTemplate(transactionManager));
    }

    OutboxRelay(Sender sender, TransactionOperations transactions) {
        this.sender = sender;
        this.transactions = transactions;
    }

    /**
     * Publishes up to {@code batchSize} pending rows, oldest first, and stops at the first failure.
     *
     * @return the number of rows published
     */
    public int publishPending(OutboxStore store, int batchSize, int maxAttempts) {
        int published = 0;
        for (int i = 0; i < Math.max(1, batchSize); i++) {
            Outcome outcome;
            try {
                outcome = transactions.execute(status -> publishNext(store, maxAttempts));
            } catch (RuntimeException e) {
                logger.warning("Outbox relay stopped; will retry on the next run: " + e.getMessage());
                break;
            }
            if (outcome != Outcome.PUBLISHED) {
                break;
            }
            published++;
        }
        return published;
    }

    private Outcome publishNext(OutboxStore store, int maxAttempts) {
        OutboxEntry entry = store.lockNextPending();
        if (entry == null) {
            return Outcome.NOTHING_PENDING;
        }
        try {
            sender.send(entry.getExchange(), entry.getRoutingKey(), entry.getMessage(), entry.getMessageId());
        } catch (RuntimeException e) {
            store.markPublishFailed(entry.getId(), e.getMessage(), maxAttempts);
            logger.warning("Outbox publish failed for " + entry.getDescription() + " (outbox id " + entry.getId()
                    + "); will retry: " + e.getMessage());
            return Outcome.FAILED;
        }
        store.delete(entry.getId());
        logger.info("Outbox message published to " + entry.getExchange() + " for " + entry.getDescription()
                + " (message id " + entry.getMessageId() + ")");
        return Outcome.PUBLISHED;
    }

    private static Sender confirmedSender(RabbitTemplate rabbitTemplate, long confirmTimeoutMs) {
        return (exchange, routingKey, message, messageId) -> rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(exchange, routingKey, message, amqpMessage -> {
                amqpMessage.getMessageProperties().setMessageId(messageId);
                return amqpMessage;
            });
            operations.waitForConfirmsOrDie(confirmTimeoutMs);
            return null;
        });
    }
}
