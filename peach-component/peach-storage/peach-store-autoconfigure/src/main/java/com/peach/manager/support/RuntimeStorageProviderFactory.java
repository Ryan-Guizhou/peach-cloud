package com.peach.manager.support;

import com.peach.config.StorageProperties;
import com.peach.storage.spi.StorageProvider;
import com.peach.storage.spi.StorageProviderFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 基于运行时配置创建临时存储 Provider 的工厂组件。
 *
 * <p>
 * 该组件用于根据动态传入的存储配置，
 * 通过 SPI 机制匹配对应的 {@link StorageProviderFactory}，
 * 创建具体的存储 Provider 实现。
 * </p>
 *
 * <p>
 * 主要用于存储管理场景，例如：
 * </p>
 *
 * <ul>
 *     <li>测试存储连接</li>
 *     <li>浏览存储内容</li>
 *     <li>验证存储配置</li>
 *     <li>动态访问不同类型存储</li>
 * </ul>
 *
 * <p>
 * 该组件根据当前操作动态创建 Provider，
 * 不负责业务文件上传下载场景中的 Provider 长期缓存。
 * </p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Slf4j
public class RuntimeStorageProviderFactory {

    private final List<StorageProviderFactory> storageProviderFactories;

    public RuntimeStorageProviderFactory(List<StorageProviderFactory> storageProviderFactories) {
        this.storageProviderFactories = storageProviderFactories;
    }

    /**
     * 根据运行时配置创建临时存储 Provider。
     *
     * <p>
     * 执行流程：
     * </p>
     *
     * <ol>
     *     <li>校验存储配置合法性</li>
     *     <li>遍历已注册的 StorageProviderFactory</li>
     *     <li>匹配支持当前存储类型的 Factory</li>
     *     <li>执行存储配置校验</li>
     *     <li>创建对应 Provider 实例</li>
     * </ol>
     *
     * @param providerConfig 运行时存储提供方配置
     * @return 存储 Provider 实例
     * @throws IllegalArgumentException 当存储配置为空或类型为空时抛出
     * @throws IllegalStateException 当不存在对应存储类型的 Factory 时抛出
     */
    public StorageProvider create(StorageProperties.StorageProvider providerConfig) {
        validate(providerConfig);
        for (StorageProviderFactory storageProviderFactory : storageProviderFactories) {
            if (storageProviderFactory.supports(providerConfig.getType())) {
                storageProviderFactory.validate(providerConfig.getName(), providerConfig);
                return storageProviderFactory.create(providerConfig);
            }
        }
        throw new IllegalStateException("No StorageProviderFactory found for type: " + providerConfig.getType());
    }

    /**
     * 校验存储提供方配置。
     *
     * <p>
     * 校验内容：
     * </p>
     *
     * <ul>
     *     <li>配置对象不能为空</li>
     *     <li>存储类型不能为空</li>
     *     <li>存储名称为空时自动生成默认名称</li>
     * </ul>
     *
     * @param providerConfig 存储提供方配置
     */
    private void validate(StorageProperties.StorageProvider providerConfig) {
        if (providerConfig == null) {
            throw new IllegalArgumentException("Provider config must not be null");
        }
        if (providerConfig.getType() == null) {
            throw new IllegalArgumentException("Provider type must not be null");
        }
        if (providerConfig.getName() == null || providerConfig.getName().isBlank()) {
            providerConfig.setName(providerConfig.getType().name().toLowerCase());
        }
    }
}
