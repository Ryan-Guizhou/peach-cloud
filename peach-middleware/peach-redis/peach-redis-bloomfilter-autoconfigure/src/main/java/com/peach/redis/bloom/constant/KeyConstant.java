package com.peach.redis.bloom.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/11/27 15:17
 */
public interface KeyConstant {

    String SEGMENT_KEY = "{0}:{1}:segments";

    String LOCK_KEY = "{0}:{1}:lock";

    String SEGMENT_NAME_KEY = "{0}:{1}:segment{2}";

    String SEGMENT_COUNT_KEY = "{0}:{1}:count";

    String CAPACITY_MAP_KEY = "{0}:{1}:capacity";

    String FPP_MAP_KEY = "{0}:{1}:fpp";
}
