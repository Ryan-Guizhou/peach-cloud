package com.peach.redis.bloom.spi;

/**
 * 扩容策略 SPI：定义段容量的估算与下一段容量的计算规则。
 */
public interface BloomScalePolicy {
    /**
     * 返回当前尾段的容量，用于估算负载；默认使用初始容量按段指数增长
     */
    long capacityOf(String segmentName, long initialCapacity, int segmentIndex);

    /**
     * 计算下一段容量
     */
    long nextCapacity(long currentCapacity, double scaleFactor);

    String getName();

    static BloomScalePolicy defaultPolicy() {
        return new BloomScalePolicy() {
            @Override
            public long capacityOf(String segmentName, long initialCapacity, int segmentIndex) {
                double cap = initialCapacity * Math.pow(2.0, Math.max(0, segmentIndex));
                return (long) Math.max(initialCapacity, cap);
            }

            @Override
            public long nextCapacity(long currentCapacity, double scaleFactor) {
                double next = currentCapacity * Math.max(1.1d, scaleFactor);
                return (long) Math.ceil(next);
            }

            @Override
            public String getName() {
                return this.getClass().getSimpleName();
            }
        };
    }
}