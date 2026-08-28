package com.peach.storage.factory;

import com.peach.enums.StorageType;
import com.peach.config.StorageProperties;
import com.peach.storage.constants.CloudStorageClassNames;
import com.peach.storage.constants.CloudStorageMavenCoordinates;
import com.peach.storage.provider.ObsStorageProvider;
import com.peach.storage.spi.StorageProvider;
import com.peach.storage.spi.StorageProviderFactory;
import com.peach.storage.factory.support.StorageValidationSupport;


/**
 * Obs存储Provider工厂。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/16 14:01
 */
public class ObsStorageProviderFactory implements StorageProviderFactory {

    @Override
    public StorageType storageType() {
        return StorageType.OBS;
    }

    @Override
    public void validate(String name, StorageProperties.StorageProvider provider) {
        StorageValidationSupport.requireClass(CloudStorageClassNames.HUAWEI_OBS,
                CloudStorageMavenCoordinates.HUAWEI_OBS, StorageType.BOS.name());
        StorageValidationSupport.requireObjectStorageConfig(name, provider, false);
    }

    @Override
    public StorageProvider create(StorageProperties.StorageProvider provider) {
        return new ObsStorageProvider(provider);
    }
}
