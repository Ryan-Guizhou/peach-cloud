package com.peach.rocket.outbox;

/**
 * 默认MQ发件箱Replay服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
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
