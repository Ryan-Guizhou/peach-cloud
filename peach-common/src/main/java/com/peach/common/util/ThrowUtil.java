package com.peach.common.util;

import com.peach.common.exception.BusinessException;

import java.util.Optional;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/13 13:05
 */
public final class ThrowUtil {

    private ThrowUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static void throwIf(boolean condition, RuntimeException exception) {
        Optional.ofNullable(condition)
                .filter(v -> !condition)
                .orElseThrow(() -> exception);
    }

    public static void throwIf(boolean condition, String message,String language) {
        throwIf(condition, new BusinessException(message,language));
    }
}
