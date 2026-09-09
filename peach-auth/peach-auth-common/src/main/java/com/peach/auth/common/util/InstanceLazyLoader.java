package com.peach.auth.common.util;

import cn.hutool.extra.spring.SpringUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class InstanceLazyLoader {

    private static final Map<Class<?>, Object> INSTANCE_MAP = new ConcurrentHashMap<>();

    private InstanceLazyLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> T getInstance(Class<T> clazz, Supplier<? extends T> supplier) {
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
        return clazz.cast(instance);
    }

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
