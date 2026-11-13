package com.transgate.api.events;

/**
 * One outbox row ready to publish: where it goes, the queue payload, and the id used to delete or retry it.
 */
public class OutboxEntry {

    private final long id;
    private final String messageId;
    private final String description;
    private final String exchange;
    private final String routingKey;
    private final Object message;

    public OutboxEntry(long id, String messageId, String description, String exchange, String routingKey, Object message) {
        this.id = id;
        this.messageId = messageId;
        this.description = description;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    /** Stable AMQP message_id so consumers can drop redeliveries of the same outbox row. */
    public String getMessageId() {
        return messageId;
    }

    public String getDescription() {
        return description;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public Object getMessage() {
        return message;
    }
}
