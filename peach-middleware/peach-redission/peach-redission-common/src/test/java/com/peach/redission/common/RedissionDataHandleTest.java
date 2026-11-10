package com.peach.redission.common;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验每种 TimeUnit 映射与本地锁稳定性。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class RedissionDataHandleTest {
    @Test
    void conversionPreservesSubsecondPrecision() {
        RedissionDataHandle handler = new RedissionDataHandle(null);
        assertThat(handler.getDuration(1200L, TimeUnit.MILLISECONDS)).isEqualTo(Duration.ofMillis(1200));
        assertThat(handler.getDuration(1200L, TimeUnit.MICROSECONDS)).isEqualTo(Duration.ofNanos(1200000L));
        assertThat(handler.getDuration(1200L, TimeUnit.NANOSECONDS)).isEqualTo(Duration.ofNanos(1200L));
        assertThat(handler.getDuration(2L, TimeUnit.SECONDS)).isEqualTo(Duration.ofSeconds(2));
        assertThat(handler.getDuration(2L, TimeUnit.MINUTES)).isEqualTo(Duration.ofMinutes(2));
        assertThat(handler.getDuration(2L, TimeUnit.HOURS)).isEqualTo(Duration.ofHours(2));
        assertThat(handler.getDuration(2L, TimeUnit.DAYS)).isEqualTo(Duration.ofDays(2));
    }

    @Test
    void localLockShouldHaveStableReference() {
        LocalCacheLock lock = new LocalCacheLock();
        assertThat(lock.getLock("customer:1", true)).isSameAs(lock.getLock("customer:1", false));
    }
}
