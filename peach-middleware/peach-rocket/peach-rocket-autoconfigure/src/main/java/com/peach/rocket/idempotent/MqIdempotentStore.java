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
}
