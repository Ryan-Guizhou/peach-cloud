package com.peach.storage.factory;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageType;
import com.peach.storage.constants.CloudStorageClassNames;
import com.peach.storage.constants.CloudStorageMavenCoordinates;
import com.peach.storage.provider.CosStorageProvider;
import com.peach.storage.spi.StorageProvider;
import com.peach.storage.spi.StorageProviderFactory;
import com.peach.storage.factory.support.StorageValidationSupport;

/**
 * Cos存储Provider工厂。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/17 10:20
 */
public class CosStorageProviderFactory implements StorageProviderFactory {

    @Override
    public StorageType storageType() {
        return StorageType.COS;
    }

    @Override
    public void validate(String name, StorageProperties.StorageProvider provider) {
        StorageValidationSupport.requireClass(CloudStorageClassNames.TENCENT_COS,
                CloudStorageMavenCoordinates.TENCENT_COS, StorageType.COS.name());
        StorageValidationSupport.requireObjectStorageConfig(name, provider, true);
    }

    @Override
    public StorageProvider create(StorageProperties.StorageProvider provider) {
        return new CosStorageProvider(provider);
    }
}
