package com.peach.common.util.encrypt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptRegisterTest {

    @Test
    void shouldRejectBlankTypeWithReadableException() {
        assertThatThrownBy(() -> EncryptRegister.getProvider(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Encrypt type must not be blank");
    }
}
