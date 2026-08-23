package com.peach.storage;

import com.peach.enums.StorageType;
import com.peach.storage.spi.StorageProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageProviderRegistryTest {

    @Test
    void shouldRejectDuplicateProviderNames() {
        assertThrows(RuntimeException.class, () -> new StorageProviderRegistry(Arrays.asList(
                new TestProvider("primary"),
                new TestProvider("primary")
        )));
    }

    @Test
    void shouldCloseProvidersOnDestroy() throws Exception {
        TestProvider provider = new TestProvider("primary");
        StorageProviderRegistry registry = new StorageProviderRegistry(List.of(provider));

        assertFalse(provider.closed.get());
        registry.destroy();
        assertTrue(provider.closed.get());
    }

    private static class TestProvider implements StorageProvider {

        private final String name;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private TestProvider(String name) {
            this.name = name;
        }

        @Override
        public String bucketName() {
            return "bucket";
        }

        @Override
        public StorageType storageType() {
            return StorageType.LOCAL;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean exists(String objectKey) {
            return false;
        }

        @Override
        public boolean exists(String bucketName, String objectKey) {
            return false;
        }

        @Override
        public com.peach.response.UploadResult upload(com.peach.request.UploadObjectRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.io.InputStream download(com.peach.request.DownloadObjectRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public com.peach.response.DeleteResult delete(com.peach.request.DeleteObjectRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String generatePresignedUrl(String objectKey, long expireSeconds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }
}
