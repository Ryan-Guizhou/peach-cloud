package com.peach.rocket.transaction;

import com.peach.rocket.annotation.MqTransaction;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.JacksonMqMessageCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqLocalTransactionState;
import com.peach.rocket.core.MqTransactionHandler;
import com.peach.rocket.route.MqRoute;
import java.util.stream.Stream;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 事务消息 Key 和本地事务结果的发送回归。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 15:00
 */
class MqTransactionSendRegressionTest {
    @MqTransaction(topic = "orders", tag = "created")
    static class Handler implements MqTransactionHandler<String> {
        public MqLocalTransactionState executeLocalTransaction(String event, String key) { return MqLocalTransactionState.COMMIT; }
        public MqLocalTransactionState checkLocalTransaction(String event, String key) { return MqLocalTransactionState.COMMIT; }
    }

    @Test
    void sendBusinessKeyUsingSystemPropertyAndReportRollback() throws Exception {
        var properties = new PeachRocketProperties();
        var rocket = new RocketMQProperties();
        var sdk = mock(TransactionMQProducer.class);
        var result = new TransactionSendResult();
        result.setSendStatus(SendStatus.SEND_OK);
        result.setLocalTransactionState(LocalTransactionState.ROLLBACK_MESSAGE);
        when(sdk.sendMessageInTransaction(any(), any())).thenReturn(result);
        @SuppressWarnings("unchecked")
        ObjectProvider<MqTransactionHandler<?>> handlers = mock(ObjectProvider.class);
        when(handlers.orderedStream()).thenAnswer(call -> Stream.of(new Handler()));
        var producer = new RocketMqTransactionMessageProducer(rocket, properties,
                new JacksonMqMessageCodec(new ObjectMapper().findAndRegisterModules()),
                (payload, options) -> new MqRoute("default-biz-orders", "created", "order-1"),
                new DefaultMqHeaderResolver(), handlers) {
            @Override
            protected TransactionMQProducer createProducer(String group) { return sdk; }
        };
        producer.start();
        try {
            var sent = producer.sendTransaction("event", "transaction-1", null);
            assertFalse(sent.isSuccess(), "A rolled back local transaction is not committed");
            verify(sdk).sendMessageInTransaction(argThat(message -> "order-1".equals(message.getKeys())), any());
        } finally {
            producer.stop();
        }
    }
}
