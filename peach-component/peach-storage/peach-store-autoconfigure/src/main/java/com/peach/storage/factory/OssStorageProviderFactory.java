package com.peach.storage.factory;

import com.peach.enums.StorageType;
import com.peach.config.StorageProperties;
import com.peach.storage.constants.CloudStorageClassNames;
import com.peach.storage.constants.CloudStorageMavenCoordinates;
import com.peach.storage.provider.OssStorageProvider;
import com.peach.storage.spi.StorageProvider;
import com.peach.storage.spi.StorageProviderFactory;
import com.peach.storage.factory.support.StorageValidationSupport;


/**
 * 阿里云 OSS provider 工厂。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class OssStorageProviderFactory implements StorageProviderFactory {

    @Override
    public StorageType storageType() {
        return StorageType.OSS;
    }

    @Override
    public void validate(String name, StorageProperties.StorageProvider provider) {
        StorageValidationSupport.requireClass(CloudStorageClassNames.ALIYUN_OSS,
                CloudStorageMavenCoordinates.ALIYUN_OSS, StorageType.BOS.name());
        StorageValidationSupport.requireObjectStorageConfig(name, provider, false);
    }

    @Override
    public StorageProvider create(StorageProperties.StorageProvider provider) {
        return new OssStorageProvider(provider);
    }
}
