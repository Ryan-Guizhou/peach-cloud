package com.peach.sample.redis.bloom.provider;


import com.peach.redis.bloom.spi.BloomScalePolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * 斐波那契增长策略：容量按斐波那契数列增长，平滑扩容
 */
public class CustomScalePolicy implements BloomScalePolicy {
    
    private final Map<Integer, Long> fibonacciCache = new HashMap<>();
    
    public CustomScalePolicy() {
        // 初始化斐波那契缓存
        fibonacciCache.put(0, 1L);
        fibonacciCache.put(1, 1L);
    }
    
    private long fibonacci(int n) {
        return fibonacciCache.computeIfAbsent(n, k -> fibonacci(k - 1) + fibonacci(k - 2));
    }
    
    @Override
    public long capacityOf(String segmentName, long initialCapacity, int segmentIndex) {
        // 斐波那契增长：初始容量 * 斐波那契数
        long fib = fibonacci(segmentIndex);
        return initialCapacity * fib;
    }
    
    @Override
    public long nextCapacity(long currentCapacity, double scaleFactor) {
        // 找到当前容量对应的斐波那契位置，计算下一个
        int currentIndex = findFibonacciIndex(currentCapacity);
        long nextFib = fibonacci(currentIndex + 1);
        return (long) (currentCapacity * ((double) nextFib / fibonacci(currentIndex)));
    }
    
    private int findFibonacciIndex(long capacity) {
        int index = 0;
        while (fibonacci(index) < capacity / 1000L) { // 假设初始容量为1000量级
            index++;
        }
        return Math.max(0, index - 1);
    }
    
    @Override
    public String getName() {
        return "fibonacci-scale-policy";
    }

}