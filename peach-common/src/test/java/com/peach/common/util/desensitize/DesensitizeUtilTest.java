package com.peach.common.util.desensitize;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DesensitizeUtilTest {

    @Test
    void sanitizeErrorMessageMasksSensitiveValuesAndBearerToken() {
        String message = "password=abc123\nAuthorization: Bearer token-value; access_key=my-access; message";

        String sanitized = DesensitizeUtil.sanitizeErrorMessage(message, 200);

        assertThat(sanitized).doesNotContain("abc123", "token-value", "my-access", "\n");
        assertThat(sanitized).contains("password=***", "Bearer ***", "access_key=***");
    }

    @Test
    void sanitizeErrorMessageLimitsLength() {
        String sanitized = DesensitizeUtil.sanitizeErrorMessage("0123456789", 4);

        assertThat(sanitized).isEqualTo("0123");
    }
}
