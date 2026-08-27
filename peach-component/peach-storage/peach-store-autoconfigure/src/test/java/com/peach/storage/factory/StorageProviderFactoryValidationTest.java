package com.peach.storage.factory;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageType;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageProviderFactoryValidationTest {

    @Test
    void shouldRequireRegionForS3CompatibleProviders() {
        StorageProperties.StorageProvider provider = new StorageProperties.StorageProvider();
        provider.setType(StorageType.S3);
        provider.setBucketName("bucket");
        provider.setEndpoint("https://s3.example.com");
        provider.setAccessKey("ak");
        provider.setSecretKey("sk");

        assertThrows(IllegalStateException.class, () -> validateS3Factory("s3", provider));
        assertThrows(IllegalStateException.class, () -> validateCephFactory("ceph", provider));
    }

    private static void validateS3Factory(String name, StorageProperties.StorageProvider provider) {
        new S3StorageProviderFactory().validate(name, provider);
    }

    private static void validateCephFactory(String name, StorageProperties.StorageProvider provider) {
        new CephStorageProviderFactory().validate(name, provider);
    }

    @Test
    void shouldRequireEndpointForOssFactory() {
        StorageProperties.StorageProvider provider = new StorageProperties.StorageProvider();
        provider.setType(StorageType.OSS);
        provider.setBucketName("bucket");
        provider.setAccessKey("ak");
        provider.setSecretKey("sk");

        assertThrows(IllegalStateException.class, () -> validateOssFactory("oss", provider));
    }

    private static void validateOssFactory(String name, StorageProperties.StorageProvider provider) {
        new OssStorageProviderFactory().validate(name, provider);
    }

    @Test
    void shouldAllowMinioWithoutRegion() {
        StorageProperties.StorageProvider provider = new StorageProperties.StorageProvider();
        provider.setType(StorageType.MINIO);
        provider.setBucketName("bucket");
        provider.setEndpoint("http://minio.example.com:9000");
        provider.setAccessKey("ak");
        provider.setSecretKey("sk");

        new MinioStorageProviderFactory().validate("minio", provider);
    }

}
