package com.transgate.api.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

class OutboxRelayTest {

    private static class FakeStore implements OutboxStore {
        final Deque<OutboxEntry> pending = new ArrayDeque<>();
        final List<Long> deleted = new ArrayList<>();
        final List<String> failures = new ArrayList<>();

        @Override
        public OutboxEntry lockNextPending() {
            return pending.peekFirst();
        }

        @Override
        public void delete(long id) {
            deleted.add(id);
            pending.removeIf(e -> e.getId() == id);
        }

        @Override
        public void markPublishFailed(long id, String error, int maxAttempts) {
            failures.add(id + ":" + error + ":" + maxAttempts);
        }
    }

    private static OutboxEntry entry(long id, String exchange) {
        return new OutboxEntry(id, "outbox-" + id, "row " + id, exchange, exchange + ".key", "payload-" + id);
    }

    @Test
    void publishesEachRowToItsOwnExchangeWithMessageIdThenDeletesIt() {
        FakeStore store = new FakeStore();
        store.pending.add(entry(1, "belema.fi.created.exchange"));
        store.pending.add(entry(2, "belema.welcome.exchange"));
        List<String> sent = new ArrayList<>();
        OutboxRelay relay = new OutboxRelay(
                (exchange, routingKey, message, messageId) -> sent.add(exchange + "|" + routingKey + "|" + message + "|" + messageId),
                TransactionOperations.withoutTransaction());

        int published = relay.publishPending(store, 20, 20);

        assertEquals(2, published);
        assertEquals(List.of(
                "belema.fi.created.exchange|belema.fi.created.exchange.key|payload-1|outbox-1",
                "belema.welcome.exchange|belema.welcome.exchange.key|payload-2|outbox-2"), sent);
        assertEquals(List.of(1L, 2L), store.deleted);
        assertTrue(store.failures.isEmpty());
    }

    @Test
    void stopsAtFirstFailureAndKeepsTheRowForRetry() {
        FakeStore store = new FakeStore();
        store.pending.add(entry(1, "ex"));
        store.pending.add(entry(2, "ex"));
        List<Object> attempted = new ArrayList<>();
        OutboxRelay relay = new OutboxRelay(
                (exchange, routingKey, message, messageId) -> {
                    attempted.add(message);
                    throw new IllegalStateException("broker down");
                },
                TransactionOperations.withoutTransaction());

        int published = relay.publishPending(store, 20, 5);

        assertEquals(0, published);
        assertEquals(List.of("payload-1"), attempted);
        assertTrue(store.deleted.isEmpty());
        assertEquals(List.of("1:broker down:5"), store.failures);
        assertEquals(2, store.pending.size());
    }

    @Test
    void publishesAtMostOneBatch() {
        FakeStore store = new FakeStore();
        for (long id = 1; id <= 5; id++) {
            store.pending.add(entry(id, "ex"));
        }
        OutboxRelay relay = new OutboxRelay((exchange, routingKey, message, messageId) -> { },
                TransactionOperations.withoutTransaction());

        int published = relay.publishPending(store, 3, 20);

        assertEquals(3, published);
        assertEquals(List.of(1L, 2L, 3L), store.deleted);
        assertEquals(2, store.pending.size());
    }

    @Test
    void doesNothingWhenOutboxIsEmpty() {
        FakeStore store = new FakeStore();
        List<Object> sent = new ArrayList<>();
        OutboxRelay relay = new OutboxRelay((exchange, routingKey, message, messageId) -> sent.add(message),
                TransactionOperations.withoutTransaction());

        assertEquals(0, relay.publishPending(store, 20, 20));
        assertTrue(sent.isEmpty());
    }
}
