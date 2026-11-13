package com.peach.rocket.consumer;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 有界消费准入及排空测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class MqConsumerAdmissionTest {

    @Test
    void shouldLimitInflightAndReleaseAfterHandlerCompletion() {
        MqConsumerAdmission admission = new MqConsumerAdmission(1, Duration.ZERO);
        admission.open();

        assertTrue(admission.tryEnter());
        assertFalse(admission.tryEnter());
        assertFalse(admission.awaitDrained(Duration.ZERO));

        admission.leave();
        assertTrue(admission.awaitDrained(Duration.ZERO));
        assertTrue(admission.tryEnter());
        admission.leave();
    }

    @Test
    void shouldRejectNewMessagesAfterStopWithoutDroppingActiveWork() {
        MqConsumerAdmission admission = new MqConsumerAdmission(1, Duration.ZERO);
        admission.open();
        assertTrue(admission.tryEnter());

        admission.close();
        assertFalse(admission.tryEnter());
        assertFalse(admission.awaitDrained(Duration.ZERO));

        admission.leave();
        assertTrue(admission.awaitDrained(Duration.ZERO));
        admission.open();
        assertTrue(admission.tryEnter());
        admission.leave();
    }

    @Test
    void shouldNotAcceptInvalidCapacityOrNegativeWait() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new MqConsumerAdmission(0, Duration.ZERO));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new MqConsumerAdmission(1, Duration.ofMillis(-1)));
    }
}
