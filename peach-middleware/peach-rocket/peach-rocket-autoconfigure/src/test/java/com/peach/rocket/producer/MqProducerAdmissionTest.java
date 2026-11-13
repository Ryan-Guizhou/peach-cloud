package com.peach.rocket.producer;

import com.peach.rocket.exception.MqException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Producer 有界准入、幂等释放和中断恢复测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class MqProducerAdmissionTest {

    @Test
    void shouldNotOverReleaseWhenPermitClosedTwice() {
        MqProducerAdmission admission = new MqProducerAdmission(1, Duration.ZERO);
        MqProducerAdmission.Permit first = admission.acquire("async");

        assertThatThrownBy(() -> admission.acquire("async")).isInstanceOf(MqException.class)
                .hasMessageContaining("inflight limit");
        first.close();
        first.close();

        MqProducerAdmission.Permit second = admission.acquire("async");
        assertThatThrownBy(() -> admission.acquire("async")).isInstanceOf(MqException.class);
        second.close();
        assertDoesNotThrow(() -> admission.acquire("sync").close());
    }

    @Test
    void shouldRejectInvalidCapacity() {
        assertThatThrownBy(() -> new MqProducerAdmission(0, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MqProducerAdmission(1, Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
