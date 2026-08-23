package com.peach.scheduler.provider;

import com.peach.scheduler.exception.SchedulerConfigurationException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 根据 Provider ID 解析 {@link SchedulingProvider} 的注册表。
 *
 * <p>该类是 Provider SPI 的关键边界。多个 Provider 可以同时存在于 Spring 容器中，但同一 Provider ID
 * 只允许注册一个实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class SchedulingProviderResolver {

    private final Map<String, SchedulingProvider> providers = new LinkedHashMap<>();

    /**
     * 创建 Provider 解析器并校验 Provider ID 冲突。
     *
     * @param candidates Spring 容器中的 Provider 集合
     * @throws SchedulerConfigurationException Provider ID 为空或重复时抛出
     */
    public SchedulingProviderResolver(Collection<SchedulingProvider> candidates) {
        if (candidates == null) {
            return;
        }
        for (SchedulingProvider provider : candidates) {
            String providerId = normalizeProviderId(provider.getProviderId());
            SchedulingProvider previous = providers.putIfAbsent(providerId, provider);
            if (previous != null && previous != provider) {
                throw new SchedulerConfigurationException(
                        "Duplicate scheduler provider id: " + providerId);
            }
        }
    }

    /**
     * 获取指定 Provider。
     *
     * @param providerId Provider 标识
     * @return 对应 Provider
     * @throws SchedulerConfigurationException Provider 不存在时抛出
     */
    public SchedulingProvider getRequired(String providerId) {
        String normalized = normalizeProviderId(providerId);
        SchedulingProvider provider = providers.get(normalized);
        if (provider == null) {
            throw new SchedulerConfigurationException(
                    "Scheduler provider is not available: " + normalized
                            + ". Registered providers: " + providers.keySet());
        }
        return provider;
    }

    /**
     * 获取已注册 Provider 的只读视图。
     *
     * @return Provider ID 到实现的只读视图
     */
    public Map<String, SchedulingProvider> getProviders() {
        return java.util.Collections.unmodifiableMap(providers);
    }

    private String normalizeProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new SchedulerConfigurationException("Scheduler provider id must not be blank");
        }
        return providerId.trim().toLowerCase(Locale.ROOT);
    }
}
