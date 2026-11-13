package com.peach.rocket.idempotent;

/**
 * 消费幂等存储扩展点，所有状态更新都必须携带有效的认领凭据。
 *
 * <p>不再提供无所有者令牌的兼容入口，防止绕过租约栅栏。</p>
 */
public interface MqIdempotentStore {

    /**
     * 原子领取事件；返回已完成、处理中或持有本次令牌的成功认领。
     *
     * @param context 幂等上下文
     * @return 认领状态与所有者凭据
     */
    MqIdempotentClaim claim(MqIdempotentContext context);

    /**
     * 仅允许有效持有者确认消费成功。
     *
     * @param context 幂等上下文
     * @param claim 认领凭据
     */
    void markSuccess(MqIdempotentContext context, MqIdempotentClaim claim);

    /**
     * 仅允许有效持有者标记处理失败。
     *
     * @param context 幂等上下文
     * @param claim 认领凭据
     */
    void markFailed(MqIdempotentContext context, MqIdempotentClaim claim);

    /** @return 是否支持有效认领续租 */
    default boolean supportsLeaseRenewal() { return false; }

    /** @return 是否跨进程持久化 */
    default boolean isDurable() { return false; }

    /** @return 是否支持所有者令牌栅栏 */
    default boolean supportsOwnerFencing() { return false; }

    /**
     * 更新有效认领的租约。
     *
     * @param context 幂等上下文
     * @param claim 认领凭据
     * @return 是否续租成功
     */
    default boolean renew(MqIdempotentContext context, MqIdempotentClaim claim) {
        return false;
    }

    /**
     * 有界清理过期终态记录，不得删除处理中记录。
     *
     * @param limit 本次清理上限
     * @return 实际清理条数
     */
    default int cleanupTerminal(int limit) { return 0; }
}
