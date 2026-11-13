package com.peach.rocket.metrics;

/**
 * RocketMQ 关键事件指标 SPI。
 *
 * <p>outcome 和 mode 必须是组件内部固定低基数枚举值，不允许输入消息 ID、业务键或 Topic。
 * 没有接入 peach-observability 时使用 NOOP，不添加运行时监控依赖。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
public interface MqMetrics {

    MqMetrics NOOP = new MqMetrics() {
    };

    /**
     * 记录发布结果。
     *
     * @param mode 发送模式（sync、async、oneway）
     * @param outcome 固定结果标签
     * @param durationNanos 耗时纳秒
     */
    default void publish(String mode, String outcome, long durationNanos) {
    }

    /**
     * 记录消费者业务回调的 ACK/失败结果。
     *
     * @param outcome 固定结果标签
     * @param durationNanos 耗时纳秒
     */
    default void consume(String outcome, long durationNanos) {
    }

    /**
     * 记录 Outbox 投递或状态更新结果。
     *
     * @param outcome 固定结果标签
     */
    default void outbox(String outcome) {
    }

    /**
     * 记录动态 Consumer 准入背压。
     */
    default void backpressure() {
    }
}
