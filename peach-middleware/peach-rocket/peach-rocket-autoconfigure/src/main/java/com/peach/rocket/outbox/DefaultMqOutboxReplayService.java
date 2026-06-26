package com.peach.rocket.outbox;

/**
 * 默认 Outbox 失败消息重放服务。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class DefaultMqOutboxReplayService implements MqOutboxReplayService {

    private final MqOutboxStore outboxStore;

    public DefaultMqOutboxReplayService(MqOutboxStore outboxStore) {
        this.outboxStore = outboxStore;
    }

    @Override
    public boolean replay(String messageId) {
        return outboxStore.replay(messageId);
    }
}
