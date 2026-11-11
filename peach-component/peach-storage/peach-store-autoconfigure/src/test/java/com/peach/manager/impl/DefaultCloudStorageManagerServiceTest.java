package com.peach.manager.impl;

import com.peach.config.StorageProperties;
import com.peach.enums.StorageType;
import com.peach.manager.support.RuntimeStorageProviderFactory;
import com.peach.request.BatchDeleteObjectsRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.response.ListObjectsResult;
import com.peach.response.ObjectInfo;
import com.peach.storage.spi.StorageProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 云目录删除边界回归测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class DefaultCloudStorageManagerServiceTest {

    @Test
    void shouldNotDeleteObjectsInSiblingDirectories() {
        StorageProvider provider = mock(StorageProvider.class);
        RuntimeStorageProviderFactory factory = mock(RuntimeStorageProviderFactory.class);
        StorageProperties.StorageProvider config = cloudConfig();
        when(factory.create(config)).thenReturn(provider);
        when(provider.list(any(ListObjectsRequest.class))).thenReturn(listOf(
                "reports/file.txt", "report-backup/one.txt"));

        new DefaultCloudStorageManagerService(factory).deleteDirectory(config, "report");

        verify(provider, never()).batchDelete(any(BatchDeleteObjectsRequest.class));
    }

    @Test
    void shouldDeleteOnlyObjectsWithinDirectoryBoundary() {
        StorageProvider provider = mock(StorageProvider.class);
        RuntimeStorageProviderFactory factory = mock(RuntimeStorageProviderFactory.class);
        StorageProperties.StorageProvider config = cloudConfig();
        when(factory.create(config)).thenReturn(provider);
        when(provider.list(any(ListObjectsRequest.class))).thenReturn(listOf(
                "report/a.txt", "reports/keep.txt", "report/.peach-dir", "report-backup/keep.txt"));

        new DefaultCloudStorageManagerService(factory).deleteDirectory(config, "report");

        ArgumentCaptor<BatchDeleteObjectsRequest> captor = ArgumentCaptor.forClass(BatchDeleteObjectsRequest.class);
        verify(provider).batchDelete(captor.capture());
        assertEquals(List.of("report/a.txt", "report/.peach-dir"), captor.getValue().getObjectKeys());
    }


    @Test
    void shouldDeleteEachCloudPageWithoutAccumulatingAllKeys() {
        StorageProvider provider = mock(StorageProvider.class);
        RuntimeStorageProviderFactory factory = mock(RuntimeStorageProviderFactory.class);
        StorageProperties.StorageProvider config = cloudConfig();
        when(factory.create(config)).thenReturn(provider);
        ListObjectsResult first = ListObjectsResult.builder()
                .items(List.of(ObjectInfo.builder().objectKey("report/first.txt").build(),
                        ObjectInfo.builder().objectKey("reports/keep.txt").build()))
                .truncated(true).nextContinuationToken("cursor-1").build();
        ListObjectsResult second = listOf("report/second.txt", "report-backup/keep.txt");
        when(provider.list(any(ListObjectsRequest.class))).thenReturn(first, second);

        new DefaultCloudStorageManagerService(factory).deleteDirectory(config, "report");

        ArgumentCaptor<BatchDeleteObjectsRequest> captor = ArgumentCaptor.forClass(BatchDeleteObjectsRequest.class);
        verify(provider, times(2)).batchDelete(captor.capture());
        assertEquals(List.of("report/first.txt"), captor.getAllValues().get(0).getObjectKeys());
        assertEquals(List.of("report/second.txt"), captor.getAllValues().get(1).getObjectKeys());
    }

    @Test
    void shouldRejectTruncatedPageWithoutNextCursor() {
        StorageProvider provider = mock(StorageProvider.class);
        RuntimeStorageProviderFactory factory = mock(RuntimeStorageProviderFactory.class);
        StorageProperties.StorageProvider config = cloudConfig();
        when(factory.create(config)).thenReturn(provider);
        when(provider.list(any(ListObjectsRequest.class)))
                .thenReturn(ListObjectsResult.builder().items(List.of()).truncated(true).build());

        assertThrows(RuntimeException.class,
                () -> new DefaultCloudStorageManagerService(factory).deleteDirectory(config, "report"));
        verify(provider, never()).batchDelete(any(BatchDeleteObjectsRequest.class));
    }

    private static ListObjectsResult listOf(String... objectKeys) {
        return ListObjectsResult.builder()
                .items(java.util.Arrays.stream(objectKeys)
                        .map(key -> ObjectInfo.builder().objectKey(key).build())
                        .toList())
                .truncated(false)
                .build();
    }

    private static StorageProperties.StorageProvider cloudConfig() {
        StorageProperties.StorageProvider config = new StorageProperties.StorageProvider();
        config.setType(StorageType.S3);
        config.setName("test-s3");
        return config;
    }
}
