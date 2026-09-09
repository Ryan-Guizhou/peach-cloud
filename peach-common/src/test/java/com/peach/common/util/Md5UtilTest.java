package com.peach.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Md5UtilTest {

    @Test
    void shouldCreateSha256HexDigest() {
        assertThat(Md5Util.sha256Hex("peach"))
                .isEqualTo("85356064d03872ac4bed179b8bbe8318ab67a9626be55d0d72288ee14e165265");
    }
}