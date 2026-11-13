package com.peach.rocket.transaction;

import com.peach.rocket.annotation.MqTransaction;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqLocalTransactionState;
import com.peach.rocket.core.MqTransactionHandler;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.route.MqRouteResolver;
import java.util.stream.Stream;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 事务 Producer 启动异常的资源回滚及关闭幂等性验证，无需外部 Broker。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class RocketMqTransactionLifecycleTest {

    @Test
    void shouldShutdownSdkWhenStartFails() throws Exception {
        TransactionMQProducer sdk = mock(TransactionMQProducer.class);
        doThrow(new IllegalStateException("SDK unavailable")).when(sdk).start();
        TestProducer producer = newProducer(sdk);

        assertThatThrownBy(producer::start).isInstanceOf(MqException.class)
                .hasMessageContaining("Failed to start");
        assertThat(producer.isRunning()).isFalse();
        verify(sdk).shutdown();
        producer.stop();
        verify(sdk).shutdown();
    }

    @Test
    void shouldResetStateBeforeRepeatedShutdown() throws Exception {
        TransactionMQProducer sdk = mock(TransactionMQProducer.class);
        TestProducer producer = newProducer(sdk);

        producer.start();
        assertThat(producer.isRunning()).isTrue();
        producer.stop();
        producer.stop();

        assertThat(producer.isRunning()).isFalse();
        verify(sdk, times(1)).shutdown();
    }

    private TestProducer newProducer(TransactionMQProducer sdk) {
        RocketMQProperties rocket = new RocketMQProperties();
        rocket.setNameServer("127.0.0.1:9876");
        PeachRocketProperties properties = new PeachRocketProperties();
        @SuppressWarnings("unchecked")
        ObjectProvider<MqTransactionHandler<?>> handlers = mock(ObjectProvider.class);
        when(handlers.orderedStream()).thenAnswer(invocation ->
                Stream.<MqTransactionHandler<?>>of(new Handler()));
        return new TestProducer(rocket, properties, mock(MqMessageCodec.class),
                mock(MqRouteResolver.class), new DefaultMqHeaderResolver(), handlers, sdk);
    }

    @MqTransaction(topic = "orders", tag = "created")
    private static final class Handler implements MqTransactionHandler<String> {
        @Override
        public MqLocalTransactionState executeLocalTransaction(String payload, String transactionKey) {
            return MqLocalTransactionState.COMMIT;
        }

        @Override
        public MqLocalTransactionState checkLocalTransaction(String payload, String transactionKey) {
            return MqLocalTransactionState.COMMIT;
        }
    }

    private static final class TestProducer extends RocketMqTransactionMessageProducer {
        private final TransactionMQProducer sdk;

        TestProducer(RocketMQProperties rocket, PeachRocketProperties properties,
                     MqMessageCodec codec, MqRouteResolver routeResolver,
                     DefaultMqHeaderResolver headerResolver, ObjectProvider<MqTransactionHandler<?>> handlers,
                     TransactionMQProducer sdk) {
            super(rocket, properties, codec, routeResolver, headerResolver, handlers);
            this.sdk = sdk;
        }

        @Override
        protected TransactionMQProducer createProducer(String group) {
            return sdk;
        }
    }
}
