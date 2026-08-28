package com.peach.storage.factory;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageType;
import com.peach.storage.constants.CloudStorageClassNames;
import com.peach.storage.constants.CloudStorageMavenCoordinates;
import com.peach.storage.provider.BosStorageProvider;
import com.peach.storage.spi.StorageProvider;
import com.peach.storage.spi.StorageProviderFactory;
import com.peach.storage.factory.support.StorageValidationSupport;

/**
 * Bos存储Provider工厂。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/17 10:20
 */
public class BosStorageProviderFactory implements StorageProviderFactory {

    @Override
    public StorageType storageType() {
        return StorageType.BOS;
    }

    @Override
    public void validate(String name, StorageProperties.StorageProvider provider) {
        StorageValidationSupport.requireClass(CloudStorageClassNames.BAIDU_BOS,
                CloudStorageMavenCoordinates.BAIDU_BOS, StorageType.BOS.name());
        StorageValidationSupport.requireObjectStorageConfig(name, provider, false);
    }

    @Override
    public StorageProvider create(StorageProperties.StorageProvider provider) {
        return new BosStorageProvider(provider);
    }

}
