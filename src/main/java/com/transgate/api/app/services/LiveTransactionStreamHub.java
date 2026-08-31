package com.transgate.api.app.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transgate.api.interfaces.TransactionsInterface;
import com.transgate.api.models.FullTransactionModel;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Single server-side poller that fans out new live transactions to SSE subscribers.
 */
@Service
public class LiveTransactionStreamHub {

    private static final Logger logger = Logger.getLogger(LiveTransactionStreamHub.class.getName());
    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private final TransactionsInterface transactionsInterface;
    private final ObjectMapper objectMapper;

    private final CopyOnWriteArrayList<StreamSubscriber> subscribers = new CopyOnWriteArrayList<>();
    private volatile String globalSince = "";

    public LiveTransactionStreamHub(TransactionsInterface transactionsInterface, ObjectMapper objectMapper) {
        this.transactionsInterface = transactionsInterface;
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe(String institutionScope) {
        String scope = institutionScope != null ? institutionScope.trim() : "";
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        StreamSubscriber subscriber = new StreamSubscriber(emitter, scope);
        subscribers.add(subscriber);

        emitter.onCompletion(() -> subscribers.remove(subscriber));
        emitter.onTimeout(() -> {
            subscribers.remove(subscriber);
            emitter.complete();
        });
        emitter.onError(ex -> subscribers.remove(subscriber));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"ok\",\"institution\":\"" + escapeJson(scope) + "\"}"));
        } catch (IOException ex) {
            subscribers.remove(subscriber);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    @Scheduled(fixedDelay = 2000)
    public void pollAndBroadcast() {
        if (subscribers.isEmpty()) {
            return;
        }
        try {
            List<FullTransactionModel> rows = transactionsInterface.PollLiveTransactions(globalSince, 100, "");
            if (rows == null || rows.isEmpty()) {
                return;
            }

            String newest = globalSince;
            for (FullTransactionModel row : rows) {
                if (row.getTransactiondate() != null && row.getTransactiondate().compareTo(newest) > 0) {
                    newest = row.getTransactiondate();
                }
            }
            if (!newest.isEmpty()) {
                globalSince = newest;
            }

            for (FullTransactionModel row : rows) {
                MetricsBucket bucket = classify(row);
                Map<String, Boolean> scopesNotified = new HashMap<>();
                for (StreamSubscriber subscriber : subscribers) {
                    if (!matchesScope(row, subscriber.institutionScope)) {
                        continue;
                    }
                    sendTransaction(subscriber, row);
                    scopesNotified.put(subscriber.institutionScope, Boolean.TRUE);
                }
                for (String scope : scopesNotified.keySet()) {
                    broadcastMetricsDelta(scope, bucket);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.INFO, "Live stream poll failed: " + ex.getMessage());
        }
    }

    private void broadcastMetricsDelta(String scope, MetricsBucket bucket) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "metrics-delta");
        payload.put("institution", scope);
        payload.put("successful", bucket.successful);
        payload.put("pending", bucket.pending);
        payload.put("failed", bucket.failed);
        payload.put("total", bucket.total());

        for (StreamSubscriber subscriber : subscribers) {
            if (!scope.equals(subscriber.institutionScope)) {
                continue;
            }
            sendEvent(subscriber, "metrics-delta", payload);
        }
    }

    private void sendTransaction(StreamSubscriber subscriber, FullTransactionModel row) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "transaction");
        payload.put("data", row);
        sendEvent(subscriber, "transaction", payload);
    }

    private void sendEvent(StreamSubscriber subscriber, String eventName, Object payload) {
        try {
            subscriber.emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(payload)));
        } catch (IOException ex) {
            subscribers.remove(subscriber);
            subscriber.emitter.completeWithError(ex);
        }
    }

    static boolean matchesScope(FullTransactionModel row, String institutionScope) {
        if (institutionScope == null || institutionScope.isEmpty()) {
            return true;
        }
        String src = row.getSrcInstitutioncode() != null ? row.getSrcInstitutioncode().trim() : "";
        String dest = row.getDestInstitutioncode() != null ? row.getDestInstitutioncode().trim() : "";
        return institutionScope.equals(src) || institutionScope.equals(dest);
    }

    static MetricsBucket classify(FullTransactionModel row) {
        String code = row.getSrcResponsecode() != null ? row.getSrcResponsecode().trim() : "";
        MetricsBucket bucket = new MetricsBucket();
        bucket.total = 1;
        if ("00".equals(code) || "10".equals(code) || "11".equals(code) || "16".equals(code)) {
            bucket.successful = 1;
        } else if ("09".equals(code)) {
            bucket.pending = 1;
        } else {
            bucket.failed = 1;
        }
        return bucket;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class StreamSubscriber {
        private final SseEmitter emitter;
        private final String institutionScope;

        private StreamSubscriber(SseEmitter emitter, String institutionScope) {
            this.emitter = emitter;
            this.institutionScope = institutionScope != null ? institutionScope : "";
        }
    }

    static final class MetricsBucket {
        int successful;
        int pending;
        int failed;
        int total;

        int total() {
            return successful + pending + failed;
        }
    }
}
