package com.peach.rocket.producer;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.route.MqRoute;
import com.peach.rocket.route.MqRouteResolver;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 异步消息必须等待实际 SDK 回调结束才释放并发额度，编码超限不能接触 Broker。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class RocketMqPublisherAdmissionTest {

    @Test
    void shouldHoldInflightUntilCallbackThenAllowNextSubmission() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        PeachRocketProperties config = new PeachRocketProperties();
        config.getProducer().setMaxInflight(1);
        config.getProducer().setInflightAcquireTimeout(Duration.ZERO);
        RocketMqPublisher publisher = publisher(template, config, new byte[]{1});

        CompletableFuture<MqSendResult> pending = publisher.publishAsync("first");
        assertThatThrownBy(() -> publisher.publishAsync("second"))
                .isInstanceOf(MqException.class).hasMessageContaining("inflight limit");

        ArgumentCaptor<SendCallback> callback = ArgumentCaptor.forClass(SendCallback.class);
        verify(template).asyncSend(anyString(), any(Message.class), callback.capture(), anyLong());
        SendResult sent = mock(SendResult.class);
        when(sent.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        callback.getValue().onSuccess(sent);

        assertThat(pending.join().isSuccess()).isTrue();
        publisher.publishAsync("third");
        verify(template, times(2)).asyncSend(anyString(), any(Message.class), any(SendCallback.class), anyLong());
    }

    @Test
    void shouldReleaseInflightOnFailureCallback() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        PeachRocketProperties config = new PeachRocketProperties();
        config.getProducer().setMaxInflight(1);
        config.getProducer().setInflightAcquireTimeout(Duration.ZERO);
        RocketMqPublisher publisher = publisher(template, config, new byte[]{1});

        CompletableFuture<MqSendResult> pending = publisher.publishAsync("first");
        ArgumentCaptor<SendCallback> callback = ArgumentCaptor.forClass(SendCallback.class);
        verify(template).asyncSend(anyString(), any(Message.class), callback.capture(), anyLong());
        callback.getValue().onException(new IllegalStateException("broker unavailable"));

        assertThatThrownBy(pending::join).hasCauseInstanceOf(IllegalStateException.class);
        publisher.publishAsync("second");
        verify(template, times(2)).asyncSend(anyString(), any(Message.class), any(SendCallback.class), anyLong());
    }

    @Test
    void shouldRejectLargeEnvelopeBeforeBrokerInteraction() {
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        PeachRocketProperties config = new PeachRocketProperties();
        config.getProducer().setMaxPayloadBytes(1);
        RocketMqPublisher publisher = publisher(template, config, new byte[]{1, 2});

        assertThatThrownBy(() -> publisher.publish("payload"))
                .isInstanceOf(MqException.class).hasMessageContaining("payload");
        verifyNoInteractions(template);
    }

    private RocketMqPublisher publisher(RocketMQTemplate template, PeachRocketProperties config, byte[] bytes) {
        MqMessageCodec codec = mock(MqMessageCodec.class);
        MqRouteResolver resolver = mock(MqRouteResolver.class);
        when(resolver.resolve(any(), any())).thenReturn(new MqRoute("orders", "created", "key"));
        when(codec.encode(any())).thenReturn(bytes);
        return new RocketMqPublisher(template, codec, resolver, new DefaultMqHeaderResolver(), config);
    }
}
