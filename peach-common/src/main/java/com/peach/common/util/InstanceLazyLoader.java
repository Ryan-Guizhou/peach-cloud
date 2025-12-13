package com.peach.common.util;

import cn.hutool.extra.spring.SpringUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 18:45
 */
public final class InstanceLazyLoader {

    public static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();


    private InstanceLazyLoader() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取实例
     *
     * @param clazz 类
     * @param supplier 创建实例的工厂方法
     * @return 创建的实例
     */
    public static <T> T getInstance(Class<T> clazz, Supplier supplier) {
        Object instance = INSTANCE_MAP.get(clazz);
        if (instance == null) {
            synchronized (INSTANCE_MAP) {
                instance = INSTANCE_MAP.get(clazz);
                if (instance == null) {
                    try {
                        instance = supplier.get();
                        INSTANCE_MAP.put(clazz, instance);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance for class: " + clazz.getName(), e);
                    }
                }
            }
        }
        return (T) instance;
    }

    /**
     * 获取实例
     *
     * @param clazz 类
     * @return 实例
     */
    public static <T> T getInstance(Class<T> clazz) {
        return getInstance(clazz, () -> {
            try {
                return SpringUtil.getBean(clazz);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create instance for class: " + clazz.getName(), e);
            }
        });
    }
}
