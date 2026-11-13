package com.peach.rocket.producer;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.route.AnnotationMqRouteResolver;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 配置边界与运行时 Key 的安全回归。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 15:00
 */
class RocketConfigurationRegressionTest {
    @Test
    void runtimeKeyMustBeLiteral() {
        var options = MqSendOptions.builder().topic("orders").key("#untrusted").build();
        assertEquals("#untrusted", new AnnotationMqRouteResolver(new PeachRocketProperties())
                .resolve("event", options).key());
    }

    @Test
    void unspecifiedTimeoutMustNotOverrideConfiguration() {
        assertEquals(0L, MqSendOptions.defaults().getTimeoutMillis());
    }

    @Test
    void rejectSubMillisecondTimeoutAndUnboundedWait() {
        var properties = new PeachRocketProperties();
        properties.getProducer().setDefaultTimeout(Duration.ofNanos(1));
        assertThrows(RuntimeException.class, properties::afterPropertiesSet);
    }

    @Test
    void groupPrefixingIsEnabledForFreshDeployments() {
        assertEquals(true, new PeachRocketProperties().getNaming().isAutoPrefixConsumerGroup());
    }

    @Test
    void rejectLegacyUnauthenticatedEncryptionAlgorithm() {
        var properties = new PeachRocketProperties();
        properties.getSecurity().setAlgorithm("AES_GCM");
        assertThrows(RuntimeException.class, properties::afterPropertiesSet);
    }

    @Test
    void rejectUnsupportedEncryptionAlgorithm() {
        var properties = new PeachRocketProperties();
        properties.getSecurity().setEnabled(true);
        properties.getSecurity().setAlgorithm("unsupported");
        assertThrows(RuntimeException.class, properties::afterPropertiesSet);
    }
}
