package com.peach.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void integerCodeConstructorKeepsProvidedCode() {
        BusinessException exception = new BusinessException(409, "conflict");

        assertThat(exception.getCode()).isEqualTo("409");
        assertThat(exception.getMsg()).isEqualTo("conflict");
    }
}
