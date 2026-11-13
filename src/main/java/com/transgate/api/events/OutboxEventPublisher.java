package com.transgate.api.events;

import com.transgate.api.app.services.OutboxEventService;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Relays {@code tbl_outbox_event} rows to RabbitMQ: right after the writing transaction commits, and on a
 * schedule for anything still PENDING (broker down, publish failed, app restarted).
 */
@Component
public class OutboxEventPublisher {

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    @Qualifier("outboxRelayExecutor")
    private Executor outboxRelayExecutor;

    @Value("${app.outbox.poll-batch-size:${app.rabbitmq.fi.outbox-poll-batch-size:20}}")
    private int batchSize;

    @Value("${app.outbox.max-attempts:${app.rabbitmq.fi.outbox-max-attempts:20}}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:${app.rabbitmq.fi.outbox-poll-ms:60000}}")
    public void publishPending() {
        outboxRelay.publishPending(outboxEventService, batchSize, maxAttempts);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onOutboxRecorded(OutboxRecordedEvent event) {
        outboxRelayExecutor.execute(this::publishPending);
    }
}
