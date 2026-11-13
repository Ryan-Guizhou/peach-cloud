package com.peach.rocket.consumer;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消费者生命周期失败回滚和幂等关闭验证，不依赖真实 Broker。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class DynamicRocketMqConsumerRegistrarLifecycleTest {

    @Test
    void shouldRollbackAlreadyStartedConsumersWhenLaterConsumerFails() throws Exception {
        DefaultMQPushConsumer first = mock(DefaultMQPushConsumer.class);
        DefaultMQPushConsumer second = mock(DefaultMQPushConsumer.class);
        doThrow(new IllegalStateException("broker unavailable")).when(second).start();

        TestRegistrar registrar = new TestRegistrar(first, second);
        registrar.setApplicationContext(context(Map.of("first", new FirstHandler(), "second", new SecondHandler())));

        assertThrows(RuntimeException.class, registrar::start);
        assertFalse(registrar.isRunning());
        verify(first).shutdown();
        verify(second).shutdown();
    }

    @Test
    void shouldCloseRemainingConsumersEvenWhenOneShutdownFails() {
        DefaultMQPushConsumer first = mock(DefaultMQPushConsumer.class);
        DefaultMQPushConsumer second = mock(DefaultMQPushConsumer.class);
        doThrow(new IllegalStateException("shutdown failed")).when(second).shutdown();

        TestRegistrar registrar = new TestRegistrar(first, second);
        registrar.setApplicationContext(context(Map.of("first", new FirstHandler(), "second", new SecondHandler())));
        registrar.start();
        assertTrue(registrar.isRunning());

        registrar.stop();
        registrar.stop();
        assertFalse(registrar.isRunning());
        verify(first, times(1)).shutdown();
        verify(second, times(1)).shutdown();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ApplicationContext context(Map<String, MqMessageHandler> handlers) {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeansOfType(MqMessageHandler.class, false, false)).thenReturn(handlers);
        return context;
    }

    private static class TestRegistrar extends DynamicRocketMqConsumerRegistrar {
        private final DefaultMQPushConsumer[] supplied;
        private int next;

        private TestRegistrar(DefaultMQPushConsumer... supplied) {
            super(new RocketMQProperties(), new PeachRocketProperties(), mock(MqConsumerInvoker.class));
            this.supplied = supplied;
        }

        @Override
        protected DefaultMQPushConsumer createConsumer(
                String beanName, MqConsumer annotation, String topic, String group, String tag) {
            return supplied[next++];
        }
    }

    @MqConsumer(topic = "orders", consumerGroup = "first-group")
    private static class FirstHandler implements MqMessageHandler<String> {
        @Override
        public void handle(String payload, MqConsumeContext context) {
        }
    }

    @MqConsumer(topic = "payments", consumerGroup = "second-group")
    private static class SecondHandler implements MqMessageHandler<String> {
        @Override
        public void handle(String payload, MqConsumeContext context) {
        }
    }
}
