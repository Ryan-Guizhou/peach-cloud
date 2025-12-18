package com.peach.redis.bloom.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BloomFilter Starter 配置项。
 *
 * 支持：
 * - 键前缀与默认命名空间控制；
 * - 初始容量与误判率（FPP）；
 * - 负载阈值与扩容倍率；
 * - 段数上限与本地缓存开关。
 */
@ConfigurationProperties(prefix = "peach.redis.bloom")
public class BloomFilterProperties {

    /**
     * Redis key 前缀，便于多租户或多应用隔离
     */
    private String keyPrefix = "bloom";

    /**
     * 初始容量（预估插入量）
     */
    private long initialCapacity = 1_000_000L;

    /**
     * 误判率（FPP）
     */
    private double falsePositiveProbability = 0.001d;

    /**
     * 当尾段负载达到该比例时扩容（0~1），建议 0.85~0.95
     */
    private double loadFactor = 0.9d;

    /**
     * 容量扩展倍数（例如 2.0 表示每次新段容量翻倍）
     */
    private double scaleFactor = 2.0d;

    /**
     * 单命名空间最大段数，防止段数量无限增长
     */
    private int maxSegments = 32;

    /**
     * 默认命名空间，如果未显式传递可使用该值
     */
    private String defaultNamespace = "default";

    /**
     * 是否启用本地缓存（段列表与 BloomFilter 句柄），提升性能
     */
    private boolean enableLocalCache = true;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public long getInitialCapacity() {
        return initialCapacity;
    }

    public void setInitialCapacity(long initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    public double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }

    public void setFalsePositiveProbability(double falsePositiveProbability) {
        this.falsePositiveProbability = falsePositiveProbability;
    }

    public double getLoadFactor() {
        return loadFactor;
    }

    public void setLoadFactor(double loadFactor) {
        this.loadFactor = loadFactor;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public int getMaxSegments() {
        return maxSegments;
    }

    public void setMaxSegments(int maxSegments) {
        this.maxSegments = maxSegments;
    }

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public boolean isEnableLocalCache() {
        return enableLocalCache;
    }

    public void setEnableLocalCache(boolean enableLocalCache) {
        this.enableLocalCache = enableLocalCache;
    }
}