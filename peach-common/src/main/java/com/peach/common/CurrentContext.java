package com.peach.common;

/**
 * Thread-local request context holder.
 */
public final class CurrentContext {

    private static final ThreadLocal<CurrentContextEntity> CONTEXT = new ThreadLocal<CurrentContextEntity>();

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
