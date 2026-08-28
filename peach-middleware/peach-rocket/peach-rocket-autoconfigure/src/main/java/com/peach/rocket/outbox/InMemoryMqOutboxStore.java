package com.peach.rocket.outbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryMQ发件箱存储。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class InMemoryMqOutboxStore implements MqOutboxStore {

    private final Map<String, MqOutboxEvent> events = new ConcurrentHashMap<String, MqOutboxEvent>();

    @Override
    public void save(MqOutboxEvent event) {
        events.put(event.messageId(), event);
    }

    @Override
    public List<MqOutboxEvent> findPending(int batchSize) {
        List<MqOutboxEvent> result = new ArrayList<MqOutboxEvent>();
        for (MqOutboxEvent event : events.values()) {
            if ((event.status() == MqOutboxStatus.INIT || event.status() == MqOutboxStatus.RETRY) && result.size() < batchSize) {
                result.add(event);
            }
        }
        return result;
    }

    @Override
    public void markSent(String messageId) {
        MqOutboxEvent event = events.get(messageId);
        if (event != null) {
            events.put(messageId, event.withStatus(MqOutboxStatus.SENT));
        }
    }

    @Override
    public void markFailed(String messageId) {
        MqOutboxEvent event = events.get(messageId);
        if (event != null) {
            events.put(messageId, event.withRetryCount(event.retryCount() + 1).withStatus(MqOutboxStatus.RETRY));
        }
    }

    @Override
    public boolean replay(String messageId) {
        MqOutboxEvent event = events.get(messageId);
        if (event == null || event.status() != MqOutboxStatus.FAILED) {
            return false;
        }
        events.put(messageId, event.withStatus(MqOutboxStatus.RETRY));
        return true;
    }
}
