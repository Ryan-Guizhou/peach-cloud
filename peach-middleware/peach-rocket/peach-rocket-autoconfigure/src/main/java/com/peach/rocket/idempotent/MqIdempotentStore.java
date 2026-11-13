package com.peach.rocket.idempotent;

/**
 * MQ幂等存储。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqIdempotentStore {

    /**
     * 尝试开始处理消息。
     *
     * @param context 幂等上下文
     * @return true 表示允许处理
     */
    boolean tryStart(MqIdempotentContext context);

    /**
     * 标记消费成功。
     *
     * @param context 幂等上下文
     */
    void markSuccess(MqIdempotentContext context);

    /**
     * 标记消费失败。
     *
     * @param context 幂等上下文
     */
    void markFailed(MqIdempotentContext context);

    /**
     * 判断消息是否已成功消费。
     *
     * @param context 幂等上下文
     * @return true 表示已成功消费
     */
    boolean isSuccess(MqIdempotentContext context);

    /**
     * 原子认领与已完成检查。旧实现默认桥接以保持兼容；
     * 需要跨实例安全性的实现必须覆盖并提供 owner token。
     *
     * @param context 幂等上下文
     * @return 本次认领状态和 owner token
     */
    default MqIdempotentClaim claim(MqIdempotentContext context) {
        if (isSuccess(context)) {
            return MqIdempotentClaim.succeeded();
        }
        return tryStart(context) ? MqIdempotentClaim.acquired(null) : MqIdempotentClaim.inProgress();
    }

    /**
     * 仅认领持有者可以确认成功，旧实现通过默认方法兼容。
     *
     * @param context 幂等上下文
     * @param claim 本次认领
     */
    default void markSuccess(MqIdempotentContext context, MqIdempotentClaim claim) {
        if (claim != null && claim.ownerToken() != null) {
            throw new IllegalStateException("Idempotent store does not support owner fencing");
        }
        markSuccess(context);
    }

    /**
     * 仅认领持有者可以结束失败，旧实现通过默认方法兼容。
     *
     * @param context 幂等上下文
     * @param claim 本次认领
     */
    default void markFailed(MqIdempotentContext context, MqIdempotentClaim claim) {
        if (claim != null && claim.ownerToken() != null) {
            throw new IllegalStateException("Idempotent store does not support owner fencing");
        }
        markFailed(context);
    }

    /** @return 是否支持认领 owner token 核对 */
    default boolean supportsOwnerFencing() {
        return false;
    }
}
