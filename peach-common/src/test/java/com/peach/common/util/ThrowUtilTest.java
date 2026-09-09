package com.peach.common.util;

import com.peach.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThrowUtilTest {

    @Test
    void shouldThrowProvidedExceptionWhenConditionIsTrue() {
        IllegalStateException exception = new IllegalStateException("bad state");

        assertThatThrownBy(() -> ThrowUtil.throwIf(true, exception))
                .isSameAs(exception);
    }

    @Test
    void shouldNotCreateSupplierExceptionWhenConditionIsFalse() {
        assertThatCode(() -> ThrowUtil.throwIf(false, () -> new IllegalStateException("should not throw")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowBusinessExceptionForMessage() {
        assertThatThrownBy(() -> ThrowUtil.throwIf(true, "business failed"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("business failed");
    }
}