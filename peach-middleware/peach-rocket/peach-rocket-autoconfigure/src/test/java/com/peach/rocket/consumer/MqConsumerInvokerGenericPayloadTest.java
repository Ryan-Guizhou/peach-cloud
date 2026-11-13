package com.peach.rocket.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.JacksonMqMessageCodec;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqMessageHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * 消费器对继承关系和嵌套泛型 Payload 的解析回归测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class MqConsumerInvokerGenericPayloadTest {

    @Test
    void shouldDecodeListPayloadFromInheritedHandler() {
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.getConsumer().setEnableIdempotent(false);
        JacksonMqMessageCodec codec = new JacksonMqMessageCodec(new ObjectMapper().findAndRegisterModules());
        MqConsumerInvoker invoker = new MqConsumerInvoker(codec,
                mock(com.peach.rocket.idempotent.MqIdempotentStore.class),
                mock(com.peach.rocket.idempotent.MqIdempotentKeyResolver.class),
                mock(com.peach.rocket.error.MqErrorHandler.class),
                mock(com.peach.rocket.error.MqExceptionClassifier.class), properties);
        MqMessageEnvelope<List<Payload>> envelope = MqMessageEnvelope.create(
                "msg-1", "orders", "created", "order-1", "app", List.class.getName(), 1,
                Map.of(), List.of(new Payload("p-1")), LocalDateTime.now());
        PayloadHandler handler = new PayloadHandler();

        invoker.invoke(handler, "handler", codec.encode(envelope), mock(MqConsumer.class), "msg-1", 0);

        assertEquals("p-1", handler.received.get(0).id());
    }

    private record Payload(String id) {
    }

    private abstract static class ParentHandler<T> implements MqMessageHandler<T> {
    }

    private static final class PayloadHandler extends ParentHandler<List<Payload>> {
        private List<Payload> received;

        @Override
        public void handle(List<Payload> message, MqConsumeContext context) {
            received = message;
        }
    }
}
