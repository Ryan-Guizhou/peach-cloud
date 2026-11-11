package com.peach.email.idempotency;

import com.peach.email.core.SendResult;

/**
 * 可原子占用幂等键的邮件存储扩展契约。
 *
 * <p>只允许认领者以相同令牌完成、标记未知或释放。跨实例接入时必须使用共享持久化存储
 * 实现原子 CAS/租约及过期；进程内默认实现仅保证单 JVM 内原子性。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
public interface ClaimingIdempotencyStore extends IdempotencyStore {

    /**
     * 原子认领待发送键。
     *
     * @param key 业务幂等键
     * @return 本次拥有者令牌；已发送、在发送中或状态未知时返回 null
     */
    String tryClaim(String key);

    /**
     * 确认成功发送。
     *
     * @param key 幂等键
     * @param claimToken 本次认领令牌
     * @param result 成功投递结果
     */
    void markSent(String key, String claimToken, SendResult result);

    /**
     * SMTP 结果未知时保留保护性占用，禁止自动重发。
     *
     * @param key 幂等键
     * @param claimToken 本次认领令牌
     */
    void markUnknown(String key, String claimToken);

    /**
     * 已确认未发送时释放本次认领。
     *
     * @param key 幂等键
     * @param claimToken 本次认领令牌
     */
    void releaseClaim(String key, String claimToken);
}
