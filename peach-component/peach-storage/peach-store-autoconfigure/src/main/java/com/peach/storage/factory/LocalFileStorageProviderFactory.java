package com.peach.storage.factory;

import com.peach.enums.StorageType;
import com.peach.config.StorageProperties;
import com.peach.storage.provider.LocalFileStorageProvider;
import com.peach.storage.spi.StorageProvider;
import com.peach.storage.spi.StorageProviderFactory;
import com.peach.storage.factory.support.StorageValidationSupport;


/**
 * LOCAL provider 工厂。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class LocalFileStorageProviderFactory implements StorageProviderFactory {

    @Override
    public StorageType storageType() {
        return StorageType.LOCAL;
    }

    @Override
    public void validate(String name, StorageProperties.StorageProvider provider) {
        StorageValidationSupport.requireLocalRootPath(name, provider);
    }

    @Override
    public StorageProvider create(StorageProperties.StorageProvider provider) {
        return new LocalFileStorageProvider(provider);
    }
}
