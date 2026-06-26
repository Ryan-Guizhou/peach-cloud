package com.peach.storage;

import com.peach.enums.StorageType;
import com.peach.exception.StorageException;
import com.peach.enums.StorageResultCode;
import com.peach.storage.spi.StorageProvider;
import com.peach.util.StorageLogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 存储 provider 注册中心。
 *
 * <p>注册中心负责按实例名称和存储类型查找 provider。名称查找用于业务指定具体实例，
 * 类型查找用于只配置了单个同类型实例的简单场景。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
@Slf4j
public class StorageProviderRegistry implements DisposableBean {

    private final Map<String, StorageProvider> providersByName;

    /**
     * 根据 provider 集合创建注册中心，provider 名称取 {@link StorageProvider#name()}。
     *
     * @param providers provider 集合
     */
    public StorageProviderRegistry(Collection<StorageProvider> providers) {
        Map<String, StorageProvider> providersMap = new LinkedHashMap<>();
        if (providers != null) {
            for (StorageProvider provider : providers) {
                if (provider == null) {
                    continue;
                }
                String name = provider.name();
                if (name == null || name.trim().isEmpty()) {
                    throw new StorageException(StorageResultCode.BAD_REQUEST,
                            "Storage provider name must not be blank");
                }
                if (providersMap.containsKey(name)) {
                    throw new StorageException(StorageResultCode.BAD_REQUEST,
                            "Duplicate storage provider name: " + name);
                }
                providersMap.put(name, provider);
            }
        }
        this.providersByName = Collections.unmodifiableMap(providersMap);
        log.info("StorageProviderRegistry initialized from collection. providers={}",
                StorageLogSanitizer.providerNames(this.providersByName));
    }

    /**
     * 根据命名 provider map 创建注册中心。
     *
     * @param providersByName provider 名称与实例映射
     */
    public StorageProviderRegistry(Map<String, StorageProvider> providersByName) {
        Map<String, StorageProvider> providersMap = new LinkedHashMap<>();
        if (providersByName != null) {
            providersByName.forEach((name, provider) -> {
                if (name == null || name.trim().isEmpty()) {
                    throw new StorageException(StorageResultCode.BAD_REQUEST,
                            "Storage provider name must not be blank");
                }
                if (provider == null) {
                    throw new StorageException(StorageResultCode.BAD_REQUEST,
                            "Storage provider must not be null: " + name);
                }
                if (providersMap.containsKey(name)) {
                    throw new StorageException(StorageResultCode.BAD_REQUEST,
                            "Duplicate storage provider name: " + name);
                }
                providersMap.put(name, provider);
            });
        }
        this.providersByName = Collections.unmodifiableMap(providersMap);
        log.info("StorageProviderRegistry initialized from map. providers={}",
                StorageLogSanitizer.providerNames(this.providersByName));
    }

    /**
     * 按名称获取 provider。
     *
     * @param name provider 名称
     * @return provider 实例
     */
    public StorageProvider getByName(String name) {
        return findByName(name).orElseThrow(() -> new StorageException(StorageResultCode.PROVIDER_NOT_FOUND,
                "Storage provider not found: " + name));
    }

    /**
     * 按名称查找 provider。
     *
     * @param name provider 名称
     * @return provider Optional
     */
    public Optional<StorageProvider> findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(providersByName.get(name.trim()));
    }

    /**
     * 按存储类型查找第一个匹配 provider。
     *
     * @param type 存储类型
     * @return provider Optional
     */
    public Optional<StorageProvider> findByType(StorageType type) {
        if (type == null) {
            return Optional.empty();
        }
        return providersByName.values().stream()
                .filter(provider -> type == provider.storageType())
                .findFirst();
    }

    /**
     * 获取全部 provider。
     *
     * @return provider 集合
     */
    public Collection<StorageProvider> providers() {
        return providersByName.values();
    }

    /**
     * 获取全部命名 provider。
     *
     * @return 不可变 provider map
     */
    public Map<String, StorageProvider> providersByName() {
        return providersByName;
    }

    /**
     * 判断注册中心是否为空。
     *
     * @return true 表示未注册 provider
     */
    public boolean isEmpty() {
        return providersByName.isEmpty();
    }

    /**
     * 关闭全部 provider 持有的底层资源。
     *
     * @throws Exception 关闭过程中出现的第一个异常
     */
    @Override
    public void destroy() throws Exception {
        Exception first = null;
        for (StorageProvider provider : providersByName.values()) {
            try {
                log.info("Closing storage provider. name={}, type={}", provider.name(), provider.storageType());
                provider.close();
            } catch (Exception ex) {
                log.error("Failed to close storage provider. name={}, type={}",
                        provider.name(), provider.storageType(), ex);
                if (first == null) {
                    first = ex;
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }
}
