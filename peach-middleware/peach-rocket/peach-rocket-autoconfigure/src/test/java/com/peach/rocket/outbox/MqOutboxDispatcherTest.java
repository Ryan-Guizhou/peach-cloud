package com.peach.rocket.outbox;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Broker 发送与 Store ACK 失败场景的回归验证。 */
class MqOutboxDispatcherTest {

    @Test
    void shouldRetryNonOkResultWithoutMarkingSentAndContinue() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        MqOutboxClaim first = claim("first");
        MqOutboxClaim second = claim("second");
        when(store.claimPending(50)).thenReturn(List.of(first, second));

        SendResult failed = mock(SendResult.class);
        when(failed.getSendStatus()).thenReturn(SendStatus.FLUSH_DISK_TIMEOUT);
        SendResult success = mock(SendResult.class);
        when(success.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(template.syncSend(anyString(), any(Message.class), anyLong()))
                .thenReturn(failed, success);

        new MqOutboxDispatcher(store, template, new PeachRocketProperties()).dispatch();

        verify(store).markFailed(first);
        verify(store, never()).markSent(first);
        verify(store).markSent(second);
    }

    @Test
    void shouldPreserveConfirmedSendWhenStoreAckFails() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        MqOutboxClaim first = claim("first");
        MqOutboxClaim second = claim("second");
        when(store.claimPending(50)).thenReturn(List.of(first, second));
        SendResult success = mock(SendResult.class);
        when(success.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(template.syncSend(anyString(), any(Message.class), anyLong())).thenReturn(success);
        doThrow(new IllegalStateException("DB ACK unavailable")).when(store).markSent(first);

        new MqOutboxDispatcher(store, template, new PeachRocketProperties()).dispatch();

        verify(store, never()).markFailed(first);
        verify(store).markSent(second);
    }

    private MqOutboxClaim claim(String id) {
        LocalDateTime now = LocalDateTime.now();
        MqOutboxEvent event = new MqOutboxEvent(
                id, new byte[]{1}, "orders", "created", id, null,
                MqOutboxStatus.SENDING, 0, now, now);
        return new MqOutboxClaim(event, "owner-" + id);
    }
}
