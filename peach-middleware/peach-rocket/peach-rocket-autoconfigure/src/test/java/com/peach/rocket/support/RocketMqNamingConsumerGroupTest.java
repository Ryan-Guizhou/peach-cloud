package com.peach.rocket.support;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 消费组命名向后兼容及显式前缀配置验证。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class RocketMqNamingConsumerGroupTest {

    @Test
    void shouldKeepExistingConsumerGroupsUnlessExplicitlyOptedIn() {
        PeachRocketProperties properties = new PeachRocketProperties();
        properties.setNamespace("dev");

        assertEquals("order-consumer", RocketMqNaming.normalizeConsumerGroup("order-consumer", properties));
        properties.getNaming().setAutoPrefixConsumerGroup(true);
        assertEquals("dev-cg-order-consumer", RocketMqNaming.normalizeConsumerGroup("order-consumer", properties));
        assertEquals("dev-cg-order-consumer",
                RocketMqNaming.normalizeConsumerGroup("dev-cg-order-consumer", properties));
    }
}
