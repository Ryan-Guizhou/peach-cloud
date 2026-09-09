package com.peach.captcha.key;

import com.peach.common.key.KeyBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaRedisKeyTest {

    @Test
    void shouldBuildCaptchaFrequencyLimitKey() {
        String key = KeyBuilder.from(CaptchaRedisKey.CAPTCHA_REQ_LIMIT, "client-1", "GET").getRealKey();

        assertThat(key).isEqualTo("PEACH:CAPTCHA:REQ:LIMIT:client-1-GET");
    }

    @Test
    void shouldBuildRunningCaptchaKeys() {
        assertThat(KeyBuilder.from(CaptchaRedisKey.RUNNING_CAPTCHA, "token-1").getRealKey())
                .isEqualTo("PEACH:CAPTCHA:RUNNING:token-1");
        assertThat(KeyBuilder.from(CaptchaRedisKey.RUNNING_CAPTCHA_SECOND, "token-2").getRealKey())
                .isEqualTo("PEACH:CAPTCHA:RUNNING:SECOND:token-2");
    }
}