package com.peach.common;

/**
 * 当前请求上下文持有器。
 *
 * <p>当前实现继续使用 {@link ThreadLocal}，以保持现有调用方式和上下文生命周期不变。
 * JDK 21 下暂不切换 ScopedValue，因为其在 JDK 21 中仍属于 Preview 特性。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/5 19:20
 */
public final class CurrentContext {

    private static final ThreadLocal<CurrentContextEntity> CONTEXT = new ThreadLocal<>();

    private CurrentContext() {
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
