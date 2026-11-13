package com.transgate.api.events;

/**
 * Raised when an outbox row is written, so the relay can publish it as soon as the transaction commits.
 */
public class OutboxRecordedEvent {
}
