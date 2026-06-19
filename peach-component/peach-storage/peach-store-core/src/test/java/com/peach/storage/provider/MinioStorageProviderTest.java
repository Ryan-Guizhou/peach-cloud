package com.peach.storage.provider;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageCapability;
import com.peach.enums.StorageType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MinioStorageProviderTest {

    @Test
    void shouldExposeMinioAdvancedCapabilities() {
        MinioStorageProvider provider = new MinioStorageProvider(minioConfig());

        Set<StorageCapability> capabilities = provider.capabilities();
        assertTrue(capabilities.contains(StorageCapability.PRESIGNED_GET_URL));
        assertTrue(capabilities.contains(StorageCapability.PRESIGNED_PUT_URL));
        assertTrue(capabilities.contains(StorageCapability.MULTIPART_UPLOAD));
        assertTrue(capabilities.contains(StorageCapability.FRONTEND_UPLOAD_TOKEN));
    }

    private StorageProperties.StorageProvider minioConfig() {
        StorageProperties.StorageProvider provider = new StorageProperties.StorageProvider();
        provider.setName("minio");
        provider.setType(StorageType.MINIO);
        provider.setBucketName("bucket");
        provider.setEndpoint("http://minio.example.com:9000");
        provider.setRegion("us-east-1");
        provider.setAccessKey("ak");
        provider.setSecretKey("sk");
        return provider;
    }
}
