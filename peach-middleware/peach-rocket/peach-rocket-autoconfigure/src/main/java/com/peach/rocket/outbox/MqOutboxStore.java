package com.peach.rocket.outbox;

import java.util.List;

/**
 * Outbox 存储扩展点，读取、确认和重试都使用携带令牌的原子认领。
 *
 * <p>不提供无 token 状态更新接口，禁止旧 Dispatcher 绕过所有者校验。</p>
 
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
public interface MqOutboxStore {

    /** @return 是否跨进程持久化 */
    default boolean isDurable() { return false; }

    /** @return 是否支持所有者令牌栅栏 */
    default boolean supportsOwnerFencing() { return false; }

    /** @return 是否支持有效认领续租 */
    default boolean supportsLeaseRenewal() { return false; }

    /**
     * 更新有效认领的租约。
     *
     * @param claim 当前认领凭据
     * @return 是否续租成功
     */
    default boolean renew(MqOutboxClaim claim) { return false; }

    /**
     * 有界清理已完成且超过保留期的记录。
     *
     * @param limit 本次清理上限
     * @return 实际清理条数
     */
    default int cleanupTerminal(int limit) { return 0; }

    /**
     * 在调用方事务中持久化事件；持久化实现应验证真实事务参与关系。
     *
     * @param event 待投递事件
     */
    void save(MqOutboxEvent event);

    /**
     * 显式保存独立事件，不承诺与业务数据的事务原子性。
     *
     * @param event 待投递事件
     */
    void saveStandalone(MqOutboxEvent event);

    /**
     * 原子领取待发事件。
     *
     * @param batchSize 最大领取数量
     * @return 本批次事件及所有者令牌
     */
    List<MqOutboxClaim> claimPending(int batchSize);

    /**
     * 仅在有效认领下确认 Broker 发送成功。
     *
     * @param claim 当前认领凭据
     */
    void markSent(MqOutboxClaim claim);

    /**
     * 仅在有效认领下记录发送失败及后续重试。
     *
     * @param claim 当前认领凭据
     */
    void markFailed(MqOutboxClaim claim);

    /**
     * 重新启用已失败事件。
     *
     * @param messageId 事件唯一标识
     * @return 是否重置成功
     */
    boolean replay(String messageId);
}
