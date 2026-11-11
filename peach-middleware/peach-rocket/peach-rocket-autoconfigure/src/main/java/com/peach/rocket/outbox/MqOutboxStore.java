package com.peach.rocket.outbox;

import java.util.List;

/**
 * MQ发件箱存储。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqOutboxStore {

    /**
     * 保存待发送消息。
     *
     * @param event Outbox 事件
     */
    void save(MqOutboxEvent event);

    /**
     * 查询待发送消息。
     *
     * @param batchSize 单批数量
     * @return 待发送消息列表
     */
    List<MqOutboxEvent> findPending(int batchSize);

    /**
     * 认领待发送消息并返回本次领取令牌，供所有结果状态更新复核。
     *
     * <p>默认实现用于保持旧 MqOutboxStore 实现的二进制/源码行为，
     * 可靠持久化 Store 必须覆盖此方法并返回独立的认领凭据。</p>
     *
     * @param batchSize 本批最大认领数量
     * @return 有界消息与认领令牌快照
     */
    default List<MqOutboxClaim> claimPending(int batchSize) {
        return findPending(batchSize).stream()
                .map(event -> new MqOutboxClaim(event, null))
                .toList();
    }

    /**
     * 标记消息发送成功。
     *
     * @param messageId 消息 ID
     */
    void markSent(String messageId);

    /**
     * 在当前认领凭据有效时确认已发送。
     *
     * @param claim 当前批次认领消息
     */
    default void markSent(MqOutboxClaim claim) {
        markSent(claim.event().messageId());
    }

    /**
     * 标记消息发送失败。
     *
     * @param messageId 消息 ID
     */
    void markFailed(String messageId);

    /**
     * 在当前认领凭据有效时标记发送失败。
     *
     * @param claim 当前批次认领消息
     */
    default void markFailed(MqOutboxClaim claim) {
        markFailed(claim.event().messageId());
    }

    /**
     * 重置失败消息。
     *
     * @param messageId 消息 ID
     * @return true 表示重置成功
     */
    boolean replay(String messageId);
}
