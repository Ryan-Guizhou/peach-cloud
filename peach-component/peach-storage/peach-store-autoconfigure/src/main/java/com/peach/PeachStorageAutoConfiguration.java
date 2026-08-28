package com.peach;

import com.peach.config.StorageProperties;
import com.peach.manager.impl.DefaultCloudStorageManagerService;
import com.peach.manager.support.RuntimeStorageProviderFactory;
import com.peach.service.MultiZoneStorage;
import com.peach.service.impl.DefaultMultiZoneStorage;
import com.peach.storage.StorageProviderRegistry;
import com.peach.storage.StorageTemplate;
import com.peach.storage.spi.StorageProvider;
import com.peach.storage.spi.StorageProviderFactory;
import com.peach.util.StorageLogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Peach存储自动配置。
 * <p>自动装配流程为：通过 Java SPI 加载所有 {@link StorageProviderFactory}，
 * 根据 {@code peach.storage.providers} 创建并注册 {@link StorageProvider}，
 * 再注入 {@link StorageTemplate} 和 {@link MultiZoneStorage} 默认实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/16 14:01
 */
@Slf4j
@Indexed
@Configuration
@ConditionalOnClass(MultiZoneStorage.class)
@ConditionalOnProperty(prefix = "peach.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties.class)
public class PeachStorageAutoConfiguration {

    /**
     * 通过 Java SPI 加载所有 provider 工厂。
     *
     * @return provider 工厂列表
     */
    @Bean
    @ConditionalOnMissingBean(name = "storageProviderFactories")
    public List<StorageProviderFactory> storageProviderFactories() {
        List<StorageProviderFactory> factories = new ArrayList<>();
        ServiceLoader.load(StorageProviderFactory.class).forEach(factories::add);
        if (factories.isEmpty()) {
            log.error("No storage providers found. Please check your configuration.");
            throw new IllegalStateException("No StorageProviderFactory loaded by Java SPI. "
                    + "Please check META-INF/services/com.peach.storage.spi.StorageProviderFactory");
        }
        List<String> storageTypes = new ArrayList<String>();
        for (StorageProviderFactory factory : factories) {
            storageTypes.add(String.valueOf(factory.storageType()));
        }
        log.info("Found {} StorageProviderFactory implementations: {}", factories.size(), storageTypes);
        return factories;
    }

    /**
     * 创建并注册所有 provider。
     *
     * @param properties starter 配置
     * @param factories SPI 加载得到的 provider 工厂
     * @return provider 注册中心
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageProviderRegistry storageProviderRegistry(StorageProperties properties,
                                                           @Qualifier("storageProviderFactories")
                                                           List<StorageProviderFactory> factories) {
        validateProperties(properties);
        log.info("Initializing storage providers. primary={}, providerNames={}",
                properties.getPrimary(), StorageLogSanitizer.providerNames(properties.getProviders()));
        return new StorageProviderRegistry(createProviders(properties, factories));
    }

    /**
     * 创建存储模板入口。
     *
     * @param properties starter 配置
     * @param registry provider 注册中心
     * @return 存储模板
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageTemplate storageTemplate(StorageProperties properties, StorageProviderRegistry registry) {
        Map<String, StorageProvider> providers = registry.providersByName();
        String primaryName = properties.getPrimary();
        StorageProvider primaryProvider = registry.findByName(primaryName).orElse(null);
        if (primaryProvider == null) {
            log.error("No primary storage provider found. primary={}, availableProviders={}",
                    primaryName, StorageLogSanitizer.providerNames(providers));
            throw new IllegalStateException("peach.storage.primary=[" + primaryName
                    + "] does not match any provider. Available providers=" + providers.keySet());
        }
        log.info("StorageTemplate initialized. primary={}, providers={}",
                primaryName, StorageLogSanitizer.providerNames(providers));
        return new StorageTemplate(primaryProvider, providers);
    }

    /**
     * 创建多区域存储默认服务。
     *
     * @param storageTemplate 存储模板
     * @param properties starter 配置
     * @return 多区域存储服务
     */
    @Bean
    @ConditionalOnMissingBean(MultiZoneStorage.class)
    public MultiZoneStorage multiZoneStorage(StorageTemplate storageTemplate, StorageProperties properties) {
        return new DefaultMultiZoneStorage(storageTemplate, properties);
    }


    /**
     * 配置运行时存储提供者工厂
     * <p>
     * 该Bean负责管理和创建各类存储提供者实例（如：本地存储、NAS、阿里云OSS、腾讯云COS、AWS S3等）。
     * 通过注入所有可用的 StorageProviderFactory 实现，实现存储类型的动态路由和实例化。
     *
     * @ConditionalOnMissingBean 注解确保在未自定义该Bean时，才使用此默认配置，
     * 便于业务方根据需要进行覆盖或扩展。
     *
     * @param storageProviderFactories 所有已注册的存储提供者工厂列表
     *                                 由Spring容器自动注入所有实现 StorageProviderFactory 接口的Bean
     * @return 运行时存储提供者工厂实例，用于后续创建具体的存储提供者
     */
    @Bean
    @ConditionalOnMissingBean(RuntimeStorageProviderFactory.class)
    public RuntimeStorageProviderFactory runtimeStorageProviderFactory(List<StorageProviderFactory> storageProviderFactories) {
        return new RuntimeStorageProviderFactory(storageProviderFactories);
    }

    /**
     * 配置默认的云存储管理器服务
     * <p>
     * 该Bean是云存储操作的核心服务类，提供了统一的存储访问接口，
     * 包括：连接测试、对象列表、上传下载、目录管理等核心功能。
     *
     * 依赖关系说明：
     * <ul>
     *   <li>依赖 RuntimeStorageProviderFactory 来获取具体的存储提供者</li>
     *   <li>通过构造方法注入，保证依赖的显式性和不可变性</li>
     * </ul>
     *
     * @ConditionalOnMissingBean 注解允许业务方自定义 CloudStorageManagerService 的实现，
     * 覆盖默认行为，实现定制化的存储管理逻辑。
     *
     * @param runtimeStorageProviderFactory 运行时存储提供者工厂
     *                                      （由上面的 runtimeStorageProviderFactory() 方法创建）
     * @return 默认云存储管理器服务实例
     */
    @Bean
    @ConditionalOnMissingBean(DefaultCloudStorageManagerService.class)
    public DefaultCloudStorageManagerService defaultCloudStorageManagerService(RuntimeStorageProviderFactory runtimeStorageProviderFactory) {
        return new DefaultCloudStorageManagerService(runtimeStorageProviderFactory);
    }


    private Map<String, StorageProvider> createProviders(StorageProperties properties,
                                                         List<StorageProviderFactory> factories) {
        Map<String, StorageProvider> providers = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();

        if (properties != null && properties.getProviders() != null) {
            for (Map.Entry<String, StorageProperties.StorageProvider> entry : properties.getProviders().entrySet()) {
                String name = entry.getKey();
                StorageProperties.StorageProvider config = entry.getValue();
                try {
                    StorageProvider provider = createProvider(name, config, factories);
                    registerProvider(providers, name, provider);
                    log.info("Storage provider initialized. name={}, config={}",
                            name, StorageLogSanitizer.providerSummary(config));
                } catch (Exception ex) {
                    String type = config == null || config.getType() == null ? "null" : config.getType().name();
                    log.error("Failed to initialize storage provider. name={}, type={}, config={}",
                            name, type, StorageLogSanitizer.providerSummary(config), ex);
                    failures.add("  - [" + name + "] type=" + type + ": " + ex.getMessage());
                }
            }
        }

        if (!failures.isEmpty()) {
            log.error("Multiple storage providers found. Please check your configuration.");
            throw new IllegalStateException("Failed to initialize " + failures.size()
                    + " storage provider(s):\n" + String.join("\n", failures));
        }
        return providers;
    }

    private StorageProvider createProvider(String name, StorageProperties.StorageProvider config,
                                           List<StorageProviderFactory> factories) {
        validateBaseConfig(name, config);
        if (config.getName() == null || config.getName().isBlank()) {
            config.setName(name);
        }
        List<StorageProviderFactory> supportedFactories = new ArrayList<>();
        for (StorageProviderFactory factory : factories) {
            if (factory.supports(config.getType())) {
                supportedFactories.add(factory);
            }
        }
        if (supportedFactories.isEmpty()) {
            log.error("No storage providers found. Please check your configuration.");
            throw new IllegalStateException("No StorageProviderFactory found for type: " + config.getType());
        }
        if (supportedFactories.size() > 1) {
            throw new IllegalStateException("Multiple StorageProviderFactory found for type: "
                    + config.getType() + ", factories=" + supportedFactories);
        }
        StorageProviderFactory factory = supportedFactories.get(0);
        log.info("Validating storage provider config. name={}, type={}, factory={}, config={}",
                name, config.getType(), factory.getClass().getSimpleName(),
                StorageLogSanitizer.providerSummary(config));
        factory.validate(name, config);
        return factory.create(config);
    }

    private void validateBaseConfig(String name, StorageProperties.StorageProvider config) {
        if (isBlank(name)) {
            throw new IllegalStateException("Provider name must not be blank");
        }
        if (config == null) {
            throw new IllegalStateException("Missing provider config for [" + name + "]");
        }
        if (config.getType() == null) {
            throw new IllegalStateException("Missing 'type' for peach.storage.providers." + name);
        }
    }

    private void registerProvider(Map<String, StorageProvider> providers, String name, StorageProvider provider) {
        if (isBlank(name)) {
            throw new IllegalStateException("Storage provider name must not be blank");
        }
        if (provider == null) {
            throw new IllegalStateException("Storage provider must not be null: " + name);
        }
        if (providers.containsKey(name)) {
            throw new IllegalStateException("Duplicate storage provider name: " + name);
        }
        providers.put(name, provider);
    }

    private void validateProperties(StorageProperties properties) {
        if (properties == null) {
            throw new IllegalStateException("StorageProperties must not be null when peach.storage.enabled=true");
        }
        properties.validateForStartup();
        if (properties.getProviders() != null) {
            for (Map.Entry<String, StorageProperties.StorageProvider> entry : properties.getProviders().entrySet()) {
                StorageProperties.StorageProvider provider = entry.getValue();
                if (provider != null && isBlank(provider.getName())) {
                    provider.setName(entry.getKey());
                }
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
