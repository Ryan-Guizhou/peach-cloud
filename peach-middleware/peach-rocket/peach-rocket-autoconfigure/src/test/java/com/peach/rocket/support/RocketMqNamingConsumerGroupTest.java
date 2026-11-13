package com.peach.rocket.support;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.exception.MqException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 新环境 Consumer Group 默认隔离、幂等规范化及显式覆盖验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class RocketMqNamingConsumerGroupTest {

    @Test
    void shouldPrefixConsumerGroupsByDefaultWithoutDoublePrefixing() {
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.setNamespace("dev");

        assertEquals("dev-cg-order-consumer",
                RocketMqNaming.normalizeConsumerGroup("order-consumer", properties));
        assertEquals("dev-cg-order-consumer",
                RocketMqNaming.normalizeConsumerGroup("dev-cg-order-consumer", properties));
    }

    @Test
    void shouldAllowExplicitlyDisablingPrefixOutsideProduction() {
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.setNamespace("dev");
        properties.getNaming().setAutoPrefixConsumerGroup(false);

        assertEquals("order-consumer",
                RocketMqNaming.normalizeConsumerGroup("order-consumer", properties));
    }

    @Test
    void shouldRejectEmbeddedNamespaceInProduction() {
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.setProduction(true);
        assertThrows(MqException.class,
                () -> RocketMqNaming.normalizeConsumerGroup("legacy%order-consumer", properties));
    }
}
