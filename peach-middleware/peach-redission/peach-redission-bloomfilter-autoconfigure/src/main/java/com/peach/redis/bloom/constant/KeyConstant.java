package com.peach.redis.bloom.constant;

/**
 * 键常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/11/27 15:17
 */
public final class KeyConstant {

    private KeyConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SEGMENT_KEY = "{0}:{1}:segments";

    public static final String LOCK_KEY = "{0}:{1}:lock";

    public static final String SEGMENT_NAME_KEY = "{0}:{1}:segment{2}";

    public static final String SEGMENT_COUNT_KEY = "{0}:{1}:count";

    public static final String CAPACITY_MAP_KEY = "{0}:{1}:capacity";

    public static final String FPP_MAP_KEY = "{0}:{1}:fpp";
}
