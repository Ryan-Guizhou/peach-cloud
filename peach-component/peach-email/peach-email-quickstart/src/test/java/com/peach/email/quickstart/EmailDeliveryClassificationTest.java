package com.peach.email.quickstart;

import com.peach.email.core.EmailContext;
import com.peach.email.core.EmailFailureKind;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.EmailTransport;
import com.peach.email.core.SendResult;
import com.peach.email.idempotency.SimpleIdempotencyStore;
import com.peach.email.retry.SimpleRetryPolicy;
import com.peach.email.router.ProviderRouter;
import com.peach.email.service.EmailSendService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证外部 SMTP 投递结果未知时不重复发送或切换 Provider。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class EmailDeliveryClassificationTest {

    @Test
    void unknownDeliveryMustStopRetryAndFallback() {
        ProviderRouter router = mock(ProviderRouter.class);
        EmailTransport primary = mock(EmailTransport.class);
        EmailTransport secondary = mock(EmailTransport.class);
        EmailContext context = mock(EmailContext.class);
        when(router.getProviderNames()).thenReturn(Set.of("first", "second"));
        when(router.orderByPriority(any())).thenReturn(List.of("first", "second"));
        when(router.getTransport("first")).thenReturn(primary);
        when(router.getTransport("second")).thenReturn(secondary);
        when(router.getContext("first")).thenReturn(context);
        when(primary.send(any(), any())).thenReturn(new SendResult("first", null, 1L,
                false, "timeout", EmailFailureKind.DELIVERY_UNKNOWN));

        EmailSendService service = new EmailSendService(router, new SimpleIdempotencyStore(),
                new SimpleRetryPolicy(3, 0L), "first");
        SendResult result = service.sendAuto(message());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureKind()).isEqualTo(EmailFailureKind.DELIVERY_UNKNOWN);
        verify(primary, times(1)).send(any(), any());
        verify(secondary, never()).send(any(), any());
    }

    @Test
    void permanentPreSendFailureMustNotRetry() {
        ProviderRouter router = mock(ProviderRouter.class);
        EmailTransport primary = mock(EmailTransport.class);
        EmailContext context = mock(EmailContext.class);
        when(router.getProviderNames()).thenReturn(Set.of("first"));
        when(router.orderByPriority(any())).thenReturn(List.of("first"));
        when(router.getTransport("first")).thenReturn(primary);
        when(router.getContext("first")).thenReturn(context);
        when(primary.send(any(), any())).thenReturn(new SendResult("first", null, 1L,
                false, "invalid credentials", EmailFailureKind.PERMANENT_BEFORE_SEND));

        SendResult result = new EmailSendService(router, new SimpleIdempotencyStore(),
                new SimpleRetryPolicy(3, 0L), "first").sendAuto(message());
        assertThat(result.isSuccess()).isFalse();
        verify(primary, times(1)).send(any(), any());
    }

    @Test
    void shouldBoundLargeRetryDelayWithoutOverflow() {
        SimpleRetryPolicy policy = new SimpleRetryPolicy(100, Long.MAX_VALUE);
        assertThat(policy.computeDelayMillis(100)).isEqualTo(300_000L);
    }

    private static EmailMessage message() {
        return EmailMessage.builder().from("sender@example.invalid")
                .to(List.of("receiver@example.invalid"))
                .subject("Test").text("Body").build();
    }
}
