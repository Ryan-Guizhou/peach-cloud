package com.peach.rocket.metrics;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 可选指标适配故障隔离层，监控故障不得改变 Broker 投递或消费 ACK 语义。
 *
 * <p>所有状态标签由业务组件使用内部常量传入；单实例最多记录一次指标故障告警。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
public final class MqMetricsGuard implements MqMetrics {

    private static final Logger log = LoggerFactory.getLogger(MqMetricsGuard.class);

    private final MqMetrics delegate;
    private final AtomicBoolean warned = new AtomicBoolean();

    private MqMetricsGuard(MqMetrics delegate) {
        this.delegate = delegate;
    }

    /**
     * 获取安全指标代理，缺少采集实现时使用 NOOP。
     *
     * @param metrics 实际采集端
     * @return 不会将指标采集异常传播至业务消息处理的适配器
     */
    public static MqMetrics of(MqMetrics metrics) {
        if (metrics == null || metrics == MqMetrics.NOOP) {
            return MqMetrics.NOOP;
        }
        return metrics instanceof MqMetricsGuard ? metrics : new MqMetricsGuard(metrics);
    }

    @Override
    public void publish(String mode, String outcome, long durationNanos) {
        try {
            delegate.publish(mode, outcome, durationNanos);
        } catch (RuntimeException ex) {
            warnOnce(ex);
        }
    }

    @Override
    public void consume(String outcome, long durationNanos) {
        try {
            delegate.consume(outcome, durationNanos);
        } catch (RuntimeException ex) {
            warnOnce(ex);
        }
    }

    @Override
    public void outbox(String outcome) {
        try {
            delegate.outbox(outcome);
        } catch (RuntimeException ex) {
            warnOnce(ex);
        }
    }

    @Override
    public void backpressure() {
        try {
            delegate.backpressure();
        } catch (RuntimeException ex) {
            warnOnce(ex);
        }
    }

    private void warnOnce(RuntimeException ex) {
        if (warned.compareAndSet(false, true)) {
            log.warn("[mq-metrics] metrics collection failed; message handling is unaffected. exception={}",
                    ex.getClass().getName());
        }
    }
}
