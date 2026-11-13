package com.transgate.api.events;

/**
 * Outbox table access used by {@link OutboxRelay}. All methods run inside the relay's transaction.
 */
public interface OutboxStore {

    /**
     * Locks and returns the oldest PENDING row, skipping rows another relay has locked, or {@code null}.
     */
    OutboxEntry lockNextPending();

    void delete(long id);

    void markPublishFailed(long id, String error, int maxAttempts);
}
