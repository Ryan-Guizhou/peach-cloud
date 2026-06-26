package com.peach.rocket.outbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的 Outbox 存储，适用于本地开发和单实例测试。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class InMemoryMqOutboxStore implements MqOutboxStore {

    private final Map<String, MqOutboxEvent> events = new ConcurrentHashMap<String, MqOutboxEvent>();

    @Override
    public void save(MqOutboxEvent event) {
        events.put(event.getMessageId(), event);
    }

    @Override
    public List<MqOutboxEvent> findPending(int batchSize) {
        List<MqOutboxEvent> result = new ArrayList<MqOutboxEvent>();
        for (MqOutboxEvent event : events.values()) {
            if ((event.getStatus() == MqOutboxStatus.INIT || event.getStatus() == MqOutboxStatus.RETRY) && result.size() < batchSize) {
                result.add(event);
            }
        }
        return result;
    }

    @Override
    public void markSent(String messageId) {
        MqOutboxEvent event = events.get(messageId);
        if (event != null) {
            event.setStatus(MqOutboxStatus.SENT);
        }
    }

    @Override
    public void markFailed(String messageId) {
        MqOutboxEvent event = events.get(messageId);
        if (event != null) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setStatus(MqOutboxStatus.RETRY);
        }
    }

    @Override
    public boolean replay(String messageId) {
        MqOutboxEvent event = events.get(messageId);
        if (event == null || event.getStatus() != MqOutboxStatus.FAILED) {
            return false;
        }
        event.setStatus(MqOutboxStatus.RETRY);
        return true;
    }
}
