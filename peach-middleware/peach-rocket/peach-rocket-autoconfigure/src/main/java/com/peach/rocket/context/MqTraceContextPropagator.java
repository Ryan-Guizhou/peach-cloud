package com.peach.rocket.context;

import java.util.Map;

/**
 * MQ追踪上下文传播器。
 * <p>RocketMQ 模块只定义中立契约，不直接依赖具体 Tracing SDK。可观测性组件可以通过该接口
 * 将当前 Trace Context 注入消息头，并在消费时恢复父上下文、创建消费者 Span。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public interface MqTraceContextPropagator {

    /** 无链路系统时使用的空实现。 */
    MqTraceContextPropagator NOOP = new NoopMqTraceContextPropagator();

    /**
     * 将当前链路上下文注入消息头。
     *
     * @param headers 可修改的消息头，不得为空
     */
    void inject(Map<String, String> headers);

    /**
     * 从消息头提取父上下文并开始消费 Span。
     *
     * @param topic 消息 Topic
     * @param headers 消息头
     * @return 必须关闭的消费链路作用域
     */
    MqTraceScope startConsumerSpan(String topic, Map<String, String> headers);

    /**
     * MQTraceScope接口。
     * <p>业务执行失败时先调用 {@link #error(Throwable)}，随后由 try-with-resources 自动关闭。</p>
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    interface MqTraceScope extends AutoCloseable {

        /** 空作用域。 */
        MqTraceScope NOOP = new NoopMqTraceScope();

        /**
         * 标记消费失败。
         *
         * @param throwable 业务处理异常
         */
        void error(Throwable throwable);

        /** 结束当前作用域和 Span。 */
        @Override
        void close();
    }

    /**
     * NoopMQTraceContextPropagator。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    final class NoopMqTraceContextPropagator implements MqTraceContextPropagator {

        @Override
        public void inject(Map<String, String> headers) {
            // Intentionally empty.
        }

        @Override
        public MqTraceScope startConsumerSpan(String topic, Map<String, String> headers) {
            return MqTraceScope.NOOP;
        }
    }

    /**
     * NoopMQTraceScope。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    final class NoopMqTraceScope implements MqTraceScope {

        @Override
        public void error(Throwable throwable) {
            // Intentionally empty.
        }

        @Override
        public void close() {
            // Intentionally empty.
        }
    }
}
