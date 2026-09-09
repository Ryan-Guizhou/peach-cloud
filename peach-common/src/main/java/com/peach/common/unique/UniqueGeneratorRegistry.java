package com.peach.common.unique;

import com.peach.common.loader.CustomServiceLoader;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ID 生成器 SPI 注册与访问中心。
 *
 * <p>负责在类加载时通过 {@link CustomServiceLoader} 扫描并注册所有 {@link UniqueGeneratorProvider} 实现，
 * 同时支持在运行时动态注册或覆盖特定算法实现。</p>
 *
 * <p>算法默认选择与兜底规则：
 * <ol>
 *   <li>优先读取 JVM 系统属性 {@code peach.common.id.type}。</li>
 *   <li>其次读取操作系统环境变量 {@code PEACH_COMMON_ID_TYPE}。</li>
 *   <li>若未配置或配置的算法不存在，自动回退到默认算法 {@link UniqueGeneratorConst#DEFAULT_TYPE} ({@code uuid})。</li>
 *   <li>若默认算法亦不可用，安全回退到注册表中的任意可用生成器，确保基础能力不中断。</li>
 * </ol>
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
@Slf4j
public final class UniqueGeneratorRegistry {

    private static final Map<String, UniqueGeneratorProvider> PROVIDER_MAP = new ConcurrentHashMap<>();

    static {
        loadProviders();
    }

    private UniqueGeneratorRegistry() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 通过 {@link CustomServiceLoader} 扫描并加载所有 {@link UniqueGeneratorProvider} 实现。
     */
    public static void loadProviders() {
        CustomServiceLoader.load(UniqueGeneratorProvider.class).forEach(provider -> {
            if (provider != null && StringUtil.isNotBlank(provider.type())) {
                PROVIDER_MAP.put(provider.type().toLowerCase().trim(), provider);
                log.debug("Registered ID generator provider: type={}", provider.type());
            }
        });
    }

    /**
     * 手动注册或覆盖生成器 Provider。
     *
     * @param provider 提供者实现，不能为空且 {@link UniqueGeneratorProvider#type()} 不能为空
     * @throws NullPointerException     当 provider 为 null 时抛出
     * @throws IllegalArgumentException 当 provider.type() 为空白时抛出
     */
    public static void register(UniqueGeneratorProvider provider) {
        Objects.requireNonNull(provider, "IdGeneratorProvider must not be null");
        if (StringUtil.isBlank(provider.type())) {
            throw new IllegalArgumentException("Provider type must not be blank");
        }
        PROVIDER_MAP.put(provider.type().toLowerCase().trim(), provider);
        log.info("Registered custom ID generator provider: type={}", provider.type());
    }

    /**
     * 获取指定算法类型的生成器实例。
     *
     * @param type 算法类型标识（大小写不敏感，如 "uuid", "nanoid", "snowflake"）
     * @return 对应的生成器实例；当传入空白时返回全局默认生成器
     * @throws IllegalArgumentException 当指定类型未注册任何可用 Provider 时抛出
     */
    public static UniqueGenerator getGenerator(String type) {
        if (StringUtil.isBlank(type)) {
            return getDefaultGenerator();
        }
        UniqueGeneratorProvider provider = PROVIDER_MAP.get(type.toLowerCase().trim());
        if (provider == null) {
            throw new IllegalArgumentException("No ID generator provider found for type: " + type);
        }
        return provider.getGenerator();
    }

    /**
     * 获取全局默认的生成器实例。
     *
     * <p>按「系统属性 &rarr; 环境变量 &rarr; 默认配置(uuid) &rarr; 任意已加载实现」顺序寻找可用的 Provider。</p>
     *
     * @return 全局默认 ID 生成器
     * @throws IllegalStateException 当 classpath 中未找到任何 ID 生成器实现时抛出
     */
    public static UniqueGenerator getDefaultGenerator() {
        String configuredType = resolveDefaultType();
        UniqueGeneratorProvider provider = PROVIDER_MAP.get(configuredType);
        if (provider == null) {
            log.warn("Configured default ID generator '{}' not found, falling back to '{}'",
                    configuredType, UniqueGeneratorConst.DEFAULT_TYPE);
            provider = PROVIDER_MAP.get(UniqueGeneratorConst.DEFAULT_TYPE);
            if (provider == null && !PROVIDER_MAP.isEmpty()) {
                provider = PROVIDER_MAP.values().iterator().next();
                log.warn("Fallback default ID generator '{}' also not found, using first available: '{}'",
                        UniqueGeneratorConst.DEFAULT_TYPE, provider.type());
            }
        }
        if (provider == null) {
            throw new IllegalStateException("No ID generator providers available in classpath");
        }
        return provider.getGenerator();
    }

    /**
     * 当前已注册的所有生成器算法类型集合。
     *
     * @return 算法名称数组
     */
    public static String[] getAvailableTypes() {
        return PROVIDER_MAP.keySet().toArray(new String[0]);
    }

    private static String resolveDefaultType() {
        String prop = System.getProperty(UniqueGeneratorConst.PROPERTY_DEFAULT_TYPE);
        if (StringUtil.isNotBlank(prop)) {
            return prop.trim().toLowerCase();
        }
        String env = System.getenv(UniqueGeneratorConst.ENV_DEFAULT_TYPE);
        if (StringUtil.isNotBlank(env)) {
            return env.trim().toLowerCase();
        }
        return UniqueGeneratorConst.DEFAULT_TYPE;
    }
}
