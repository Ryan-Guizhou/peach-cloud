package com.peach.storage.factory;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageType;
import org.junit.jupiter.api.Test;

import java.util.Collections;

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

        assertThrows(IllegalStateException.class, () -> new S3StorageProviderFactory().validate("s3", provider));
        assertThrows(IllegalStateException.class, () -> new CephStorageProviderFactory().validate("ceph", provider));
    }

    @Test
    void shouldRequireEndpointForOssFactory() {
        StorageProperties.StorageProvider provider = new StorageProperties.StorageProvider();
        provider.setType(StorageType.OSS);
        provider.setBucketName("bucket");
        provider.setAccessKey("ak");
        provider.setSecretKey("sk");

        assertThrows(IllegalStateException.class, () -> new OssStorageProviderFactory().validate("oss", provider));
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
