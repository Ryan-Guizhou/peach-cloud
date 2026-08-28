package com.peach.common;

/**
 * 当前上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/5 19:20
 */
public final class CurrentContext {

    private static final ThreadLocal<CurrentContextEntity> CONTEXT = new ThreadLocal<CurrentContextEntity>();

    private CurrentContext() {
        throw new IllegalStateException("Utility class");
    }

    public static void setCurrentContext(CurrentContextEntity currentContextEntity) {
        CONTEXT.set(currentContextEntity);
    }

    public static CurrentContextEntity getCurrentContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
