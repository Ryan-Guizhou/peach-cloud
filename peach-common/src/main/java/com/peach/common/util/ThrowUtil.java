package com.peach.common.util;

import com.peach.common.exception.BusinessException;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 异常抛出工具类（基于 JDK 21 重构）
 * <p>
 * 提供流式条件断言与业务异常抛出能力，替代冗长的 if...throw 逻辑。
 * </p>
 *
 * @author Mr Shu
 * @version 1.1.0
 * @since 2025/12/13 13:05
 */
public final class ThrowUtil {

    private ThrowUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 当条件为 true 时抛出指定的运行时异常
     *
     * @param condition 断言条件
     * @param exception 待抛出的异常实例
     */
    public static void throwIf(boolean condition, RuntimeException exception) {
        if (condition) {
            throw Objects.requireNonNull(exception, "exception must not be null");
        }
    }

    /**
     * 当条件为 true 时，延迟构建并抛出指定的运行时异常（推荐：避免提前创建异常对象的开销）
     *
     * @param condition         断言条件
     * @param exceptionSupplier 异常构建函数，例如 {@code () -> new BusinessException("错误")}
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void throwIf(boolean condition, Supplier<X> exceptionSupplier) {
        if (condition) {
            Objects.requireNonNull(exceptionSupplier, "exceptionSupplier must not be null");
            throw exceptionSupplier.get();
        }
    }

    /**
     * 当条件为 true 时抛出 BusinessException 异常（仅带错误信息）
     *
     * @param condition 断言条件
     * @param message   错误提示信息
     */
    public static void throwIf(boolean condition, String message) {
        throwIf(condition, () -> new BusinessException(message));
    }

    /**
     * 当条件为 true 时抛出 BusinessException 异常（带错误信息和语言）
     *
     * @param condition 断言条件
     * @param message   错误提示信息
     * @param language  语言标识
     */
    public static void throwIf(boolean condition, String message, String language) {
        throwIf(condition, () -> new BusinessException(message, language));
    }

    /**
     * 当条件为 true 时抛出 BusinessException 异常（带错误码和错误信息）
     *
     * @param condition 断言条件
     * @param code      错误码
     * @param message   错误提示信息
     */
    public static void throwIf(boolean condition, Integer code, String message) {
        throwIf(condition, () -> new BusinessException(code, message));
    }

    /**
     * 当对象为 null 时抛出指定的运行时异常
     *
     * @param object    待校验的对象
     * @param exception 待抛出的异常实例
     */
    public static void throwIfNull(Object object, RuntimeException exception) {
        throwIf(object == null, exception);
    }

    /**
     * 当对象为 null 时抛出 BusinessException 异常
     *
     * @param object  待校验的对象
     * @param message 错误提示信息
     */
    public static void throwIfNull(Object object, String message) {
        throwIf(object == null, message);
    }

    /**
     * 当对象为 null 时，延迟构建并抛出指定的运行时异常
     *
     * @param object            待校验的对象
     * @param exceptionSupplier 异常构建函数
     * @param <X>               异常类型
     */
    public static <X extends RuntimeException> void throwIfNull(Object object, Supplier<X> exceptionSupplier) {
        throwIf(object == null, exceptionSupplier);
    }
}
