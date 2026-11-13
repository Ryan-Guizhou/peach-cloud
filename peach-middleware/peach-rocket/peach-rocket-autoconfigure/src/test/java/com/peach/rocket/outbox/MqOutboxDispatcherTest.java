package com.peach.rocket.outbox;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.core.MqDelay;
import org.springframework.messaging.Message;
import org.mockito.ArgumentCaptor;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Broker 发送与 Store ACK 失败场景的回归验证。 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
class MqOutboxDispatcherTest {

    @Test
    void shouldRetryNonOkResultWithoutMarkingSentAndContinue() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        MqOutboxClaim first = claim("first");
        MqOutboxClaim second = claim("second");
        when(store.claimPending(1)).thenReturn(List.of(first), List.of(second), List.of());

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
        when(store.claimPending(1)).thenReturn(List.of(first), List.of(second), List.of());
        SendResult success = mock(SendResult.class);
        when(success.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(template.syncSend(anyString(), any(Message.class), anyLong())).thenReturn(success);
        doThrow(new IllegalStateException("DB ACK unavailable")).when(store).markSent(first);

        new MqOutboxDispatcher(store, template, new PeachRocketProperties()).dispatch();

        verify(store, never()).markFailed(first);
        verify(store).markSent(second);
        var deliveryOrder = org.mockito.Mockito.inOrder(store, template);
        deliveryOrder.verify(store).claimPending(1);
        deliveryOrder.verify(template).syncSend(anyString(), any(Message.class), anyLong());
        deliveryOrder.verify(store).markSent(first);
        deliveryOrder.verify(store).claimPending(1);
        deliveryOrder.verify(template).syncSend(anyString(), any(Message.class), anyLong());
        deliveryOrder.verify(store).markSent(second);
    }

    @Test
    void shouldSubmitNormalOutboxClaimsToManagedExecutor() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        ExecutorService executor = mock(ExecutorService.class);
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.getOutbox().setParallelEnabled(true);
        MqOutboxClaim first = claim("parallel");
        when(store.claimPending(1)).thenReturn(List.of(first), List.of());
        SendResult sent = mock(SendResult.class);
        when(sent.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(template.syncSend(anyString(), any(Message.class), anyLong())).thenReturn(sent);
        when(executor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return CompletableFuture.completedFuture(null);
        });

        new MqOutboxDispatcher(store, template, properties, executor).dispatch();

        verify(executor).submit(any(Runnable.class));
        verify(store).markSent(first);
    }

    @Test
    void shouldReturnRejectedOutboxClaimForRetryWithoutSending() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        ExecutorService executor = mock(ExecutorService.class);
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.getOutbox().setParallelEnabled(true);
        MqOutboxClaim claim = claim("rejected");
        when(store.claimPending(1)).thenReturn(List.of(claim), List.of());
        when(executor.submit(any(Runnable.class))).thenThrow(new RejectedExecutionException("group saturated"));

        new MqOutboxDispatcher(store, template, properties, executor).dispatch();

        verify(store).markFailed(claim);
        verify(store, never()).markSent(claim);
        verify(template, never()).syncSend(anyString(), any(Message.class), anyLong());
    }

    @Test
    void shouldKeepShardingKeyMessagesOnSerialPath() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        ExecutorService executor = mock(ExecutorService.class);
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.getOutbox().setParallelEnabled(true);
        MqOutboxClaim original = claim("ordered");
        MqSendOptions opts = new MqSendOptions();
        opts.setShardingKey("order-1");
        MqOutboxEvent e = original.event();
        MqOutboxClaim ordered = new MqOutboxClaim(new MqOutboxEvent(e.messageId(), e.body(),
                e.topic(), e.tag(), e.businessKey(), opts, e.status(), e.retryCount(),
                e.createdAt(), e.updatedAt()), original.claimToken());
        when(store.claimPending(1)).thenReturn(List.of(ordered), List.of());
        SendResult sent = mock(SendResult.class);
        when(sent.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(template.syncSendOrderly(anyString(), any(Message.class), eq("order-1"), anyLong()))
                .thenReturn(sent);

        new MqOutboxDispatcher(store, template, properties, executor).dispatch();

        verify(executor, never()).submit(any(Runnable.class));
        verify(template).syncSendOrderly(anyString(), any(Message.class), eq("order-1"), anyLong());
        verify(store).markSent(ordered);
    }

    @Test
    void shouldSendOrderlyOutboxUsingShardingKeyAndKeepBusinessHeaders() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        MqSendOptions opts = new MqSendOptions();
        opts.setShardingKey("order-1");
        opts.setTimeoutMillis(3500L);
        opts.setHeaders(Map.of("x-request-id", "req-1"));
        MqOutboxClaim claim = withOptions("ordered", opts);
        when(store.claimPending(1)).thenReturn(List.of(claim), List.of());
        SendResult success = mock(SendResult.class);
        when(success.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(template.syncSendOrderly(anyString(), any(Message.class), eq("order-1"), eq(3500L)))
                .thenReturn(success);

        new MqOutboxDispatcher(store, template, new PeachRocketProperties()).dispatch();

        ArgumentCaptor<Message> captured = ArgumentCaptor.forClass(Message.class);
        verify(template).syncSendOrderly(anyString(), captured.capture(), eq("order-1"), eq(3500L));
        assertEquals("req-1", captured.getValue().getHeaders().get("x-request-id"));
        assertEquals("ordered", captured.getValue().getHeaders().get("KEYS"));
        verify(store).markSent(claim);
    }

    @Test
    void shouldSendDelayedOutboxUsingRocketMqDelayLevel() {
        MqOutboxStore store = mock(MqOutboxStore.class);
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        MqSendOptions options = new MqSendOptions();
        options.setDelay(MqDelay.level(3));
        MqOutboxClaim claim = withOptions("delayed", options);
        when(store.claimPending(1)).thenReturn(List.of(claim), List.of());
        SendResult success = mock(SendResult.class);
        when(success.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(template.syncSend(anyString(), any(Message.class), anyLong(), eq(3))).thenReturn(success);

        new MqOutboxDispatcher(store, template, new PeachRocketProperties()).dispatch();

        verify(template).syncSend(anyString(), any(Message.class), anyLong(), eq(3));
        verify(store).markSent(claim);
    }

    private MqOutboxClaim withOptions(String id, MqSendOptions options) {
        MqOutboxEvent original = claim(id).event();
        MqOutboxEvent withOptions = new MqOutboxEvent(original.messageId(), original.body(),
                original.topic(), original.tag(), original.businessKey(), options, original.status(),
                original.retryCount(), original.createdAt(), original.updatedAt());
        return new MqOutboxClaim(withOptions, "owner-" + id);
    }

    private MqOutboxClaim claim(String id) {
        LocalDateTime now = LocalDateTime.now();
        MqOutboxEvent event = new MqOutboxEvent(
                id, new byte[]{1}, "orders", "created", id, null,
                MqOutboxStatus.SENDING, 0, now, now);
        return new MqOutboxClaim(event, "owner-" + id);
    }
}
