package com.peach.redis.bloom.core;

import java.util.Collection;

/**
 * BloomFilter 服务接口，封装命名空间级增查与批处理能力。
 * 写入统一进入尾段（最新段），读取跨段（新→旧）。
 */
public interface BloomFilterService {

    /**
     * 写入一个值到指定命名空间的尾段。
     * @return true 表示该值此前可能不存在（近似语义）
     */
    boolean add(String namespace, Object value);

    /**
     * 判断一个值是否可能存在（跨所有段）。
     */
    boolean mightContain(String namespace, Object value);

    /**
     * 批量写入，计数用于扩容判断（近似）。
     */
    void addAll(String namespace, Collection<?> values);

    /**
     * 批量判断所有值是否均可能存在。
     */
    boolean mightContainAll(String namespace, Collection<?> values);

    /**
     * 清理命名空间下的所有段与计数。
     */
    void clear(String namespace);

    /**
     * 返回当前命名空间的段数量
     */
    int segments(String namespace);

    /**
     * 初始化命名空间的首段（仅当不存在时生效），可覆盖容量与 FPP。
     */
    void initNamespace(String namespace, Long initialCapacityOverride, Double falsePositiveProbabilityOverride);

    /**
     * 查询指定命名空间的状态信息（近似计数、容量、段数、综合 FPP 等）。
     */
    BloomStatus status(String namespace);
}