package com.peach.storage;

import com.peach.enums.StorageType;
import com.peach.request.HeadObjectRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.storage.spi.StorageProvider;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageTemplateTest {

    @Test
    void shouldRouteHeadAndListToNamedProvider() {
        TrackingProvider primary = new TrackingProvider("primary");
        TrackingProvider archive = new TrackingProvider("archive");
        Map<String, StorageProvider> providers = new LinkedHashMap<>();
        providers.put("primary", primary);
        providers.put("archive", archive);

        StorageTemplate template = new StorageTemplate(primary, providers);

        ObjectInfo info = template.head("archive", HeadObjectRequest.builder().objectKey("docs/a.txt").build());
        ListObjectsResult listResult = template.list("archive", ListObjectsRequest.builder().prefix("docs").build());

        assertEquals("archive", info.getProviderName());
        assertEquals("archive", listResult.getProviderName());
        assertEquals("docs/a.txt", archive.lastHeadObjectKey);
    }

    @Test
    void shouldRejectUnknownProvider() {
        TrackingProvider primary = new TrackingProvider("primary");
        StorageTemplate template = new StorageTemplate(primary, Map.of("primary", primary));

        assertThrows(IllegalArgumentException.class, () -> template.provider("missing"));
    }

    private static class TrackingProvider implements StorageProvider {

        private final String name;
        private String lastHeadObjectKey;

        private TrackingProvider(String name) {
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
        public ObjectInfo head(HeadObjectRequest request) {
            lastHeadObjectKey = request.getObjectKey();
            return ObjectInfo.builder().providerName(name).bucketName("bucket").objectKey(request.getObjectKey()).build();
        }

        @Override
        public ListObjectsResult list(ListObjectsRequest request) {
            return ListObjectsResult.builder()
                    .providerName(name)
                    .bucketName("bucket")
                    .prefix(request.getPrefix())
                    .items(List.of(
                            ObjectInfo.builder().providerName(name).bucketName("bucket").objectKey("docs/a.txt").build()))
                    .truncated(false)
                    .build();
        }
    }
}
