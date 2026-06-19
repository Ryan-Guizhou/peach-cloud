package com.peach.storage.spi;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageType;

/**
 * 存储 provider 工厂 SPI。
 *
 * <p>负责 provider 类型绑定、配置校验和实例创建。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public interface StorageProviderFactory {

    /**
     * 返回当前工厂支持的存储类型。
     *
     * @return 存储类型
     */
    StorageType storageType();

    /**
     * 判断当前工厂是否支持指定存储类型。
     *
     * @param type 存储类型
     * @return true 表示支持
     */
    default boolean supports(StorageType type) {
        return type != null && storageType() == type;
    }

    /**
     * 校验 provider 配置是否合法。
     *
     * @param name provider 名称
     * @param provider provider 配置
     */
    default void validate(String name, StorageProperties.StorageProvider provider) {
    }

    /**
     * 根据配置创建 provider 实例。
     *
     * @param provider provider 配置
     * @return provider 实例
     */
    StorageProvider create(StorageProperties.StorageProvider provider);
}
