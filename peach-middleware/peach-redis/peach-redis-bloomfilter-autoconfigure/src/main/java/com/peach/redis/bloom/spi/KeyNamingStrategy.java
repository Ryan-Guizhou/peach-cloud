package com.peach.redis.bloom.spi;



import com.peach.redis.bloom.constant.KeyConstant;

import java.text.MessageFormat;

/**
 * Redis 键命名策略 SPI：为不同环境/规范提供可插拔的键命名规则。
 */
public interface KeyNamingStrategy {
    /** 段列表键：存储命名空间下的所有段名（RList） */
    String segmentsKey(String prefix, String namespace);
    /** 命名空间锁键：保护初始化与扩容的并发（RLock） */
    String lockKey(String prefix, String namespace);
    /** 单段的 BloomFilter 键名 */
    String segmentName(String prefix, String namespace, int index);
    /** 单段计数键：记录近似写入数量（RAtomicLong） */
    String segmentCountKey(String prefix, String namespace, String segmentName);
    /** 容量元数据映射键：记录每段实际容量（RMap） */
    String capacityMapKey(String prefix, String namespace);
    /** FPP 元数据映射键：记录每段的误报率（RMap） */
    String fppMapKey(String prefix, String namespace);

    String getName();

    static KeyNamingStrategy defaultStrategy() {
        return new KeyNamingStrategy() {
            @Override
            public String segmentsKey(String prefix, String namespace) {
                return MessageFormat.format(KeyConstant.SEGMENT_KEY, prefix, namespace);
            }

            @Override
            public String lockKey(String prefix, String namespace) {
                return MessageFormat.format(KeyConstant.LOCK_KEY, prefix, namespace);
            }

            @Override
            public String segmentName(String prefix, String namespace, int index) {
                return MessageFormat.format(KeyConstant.SEGMENT_NAME_KEY, prefix, namespace, index);
            }

            @Override
            public String segmentCountKey(String prefix, String namespace, String segmentName) {
                return MessageFormat.format(KeyConstant.SEGMENT_COUNT_KEY, prefix, namespace, segmentName);
            }

            @Override
            public String capacityMapKey(String prefix, String namespace) {
                return MessageFormat.format(KeyConstant.CAPACITY_MAP_KEY, prefix, namespace);
            }

            @Override
            public String fppMapKey(String prefix, String namespace) {
                return MessageFormat.format(KeyConstant.FPP_MAP_KEY, prefix, namespace);
            }

            @Override
            public String getName() {
                return this.getClass().getSimpleName();
            }
        };
    }
}