package com.peach.common.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Base64UtilTest {

    @Test
    void shouldEncodeAndDecodeStandardBase64String() {
        String encoded = Base64Util.encodeToString("peach-common".getBytes(StandardCharsets.UTF_8));

        assertThat(encoded).isEqualTo("cGVhY2gtY29tbW9u");
        assertThat(new String(Base64Util.decodeFromString(encoded), StandardCharsets.UTF_8)).isEqualTo("peach-common");
    }

    @Test
    void shouldReturnEmptyBytesForBlankDecodeInput() {
        assertThat(Base64Util.decodeFromString(" ")).isEmpty();
        assertThat(Base64Util.safeDecodeFromString(null)).isEmpty();
    }
}