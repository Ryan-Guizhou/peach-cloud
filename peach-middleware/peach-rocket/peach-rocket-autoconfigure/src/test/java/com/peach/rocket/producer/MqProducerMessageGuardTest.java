package com.peach.rocket.producer;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.exception.MqException;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 编码体积、UTF-8 Header 与业务 Key 的预算校验。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
class MqProducerMessageGuardTest {

    @Test
    void shouldRejectOversizedEncodedPayload() {
        PeachRocketProperties.Producer props = new PeachRocketProperties.Producer();
        props.setMaxPayloadBytes(2);
        assertThatThrownBy(() -> MqProducerMessageGuard.validate(new byte[3], Map.of(), null, props))
                .isInstanceOf(MqException.class).hasMessageContaining("payload");
        assertThatThrownBy(() -> MqProducerMessageGuard.validate(null, Map.of(), null, props))
                .isInstanceOf(MqException.class);
        assertDoesNotThrow(() -> MqProducerMessageGuard.validate(new byte[2], Map.of(), null, props));
    }

    @Test
    void shouldCountBusinessKeyAndMultibyteHeaderValues() {
        PeachRocketProperties.Producer props = new PeachRocketProperties.Producer();
        props.setMaxHeaderCount(1);
        assertThatThrownBy(() -> MqProducerMessageGuard.validate(new byte[]{1},
                Map.of("x-trace", "t"), "business-key", props))
                .isInstanceOf(MqException.class).hasMessageContaining("count");

        props.setMaxHeaderCount(4);
        props.setMaxHeaderBytes(6);
        assertThatThrownBy(() -> MqProducerMessageGuard.validate(new byte[]{1},
                Map.of("a", "汉字"), null, props))
                .isInstanceOf(MqException.class).hasMessageContaining("bytes");
    }

    @Test
    void shouldRejectBlankHeaderNamesAndNullValues() {
        PeachRocketProperties.Producer props = new PeachRocketProperties.Producer();
        assertThatThrownBy(() -> MqProducerMessageGuard.validate(new byte[]{1},
                Map.of(" ", "value"), null, props))
                .isInstanceOf(MqException.class).hasMessageContaining("header key");
    }
}
