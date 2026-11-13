package com.peach.rocket.consumer;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentStore;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 长任务租约续期与失租保护。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 15:00
 */
class MqLeaseRenewerTest {

    @Test
    void failedRenewalPreventsSuccessfulConfirmation() throws Exception {
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.getJdbc().setProcessingLease(Duration.ofMillis(60));
        MqIdempotentStore store = mock(MqIdempotentStore.class);
        when(store.supportsLeaseRenewal()).thenReturn(true);
        CountDownLatch renewed = new CountDownLatch(1);
        when(store.renew(any(), any())).thenAnswer(call -> {
            renewed.countDown();
            return false;
        });
        MqIdempotentContext context = new MqIdempotentContext(
                "event-1", "group", "topic", null, null, "event-1", Duration.ofHours(1));
        try (MqLeaseRenewer scheduler = new MqLeaseRenewer(store, properties);
                MqLeaseRenewer.Lease lease = scheduler.open(context, MqIdempotentClaim.acquired("owner"))) {
            assertTrue(renewed.await(2, TimeUnit.SECONDS));
            assertThrows(RuntimeException.class, lease::checkValid);
        }
    }
}
