package com.peach.storage.spi.support;

import com.peach.exception.StorageException;
import com.peach.request.CopyObjectRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.response.ListObjectsResult;
import com.peach.storage.spi.StorageProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 SDK 递归复制必须拒绝错误游标与源目录内目标路径，且不能无限扫描。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class StorageRecursiveFallbackGuardTest {

    @Test
    void mustRejectRecursiveTargetBeforeAnyProviderList() {
        StorageProvider provider = mock(StorageProvider.class);
        when(provider.rawObjectKey("files")).thenReturn("files");
        when(provider.rawObjectKey("files/archive")).thenReturn("files/archive");

        assertThrows(StorageException.class,
                () -> StorageProviderSupport.copy(provider, CopyObjectRequest.builder()
                        .sourceObjectKey("files")
                        .targetObjectKey("files/archive")
                        .recursive(true).build()));

        verify(provider, never()).list(any(ListObjectsRequest.class));
    }

    @Test
    void mustRejectTruncatedPageWithStalledCursor() {
        StorageProvider provider = mock(StorageProvider.class);
        when(provider.rawObjectKey("files")).thenReturn("files");
        when(provider.rawObjectKey("archive")).thenReturn("archive");
        when(provider.list(any(ListObjectsRequest.class))).thenReturn(
                ListObjectsResult.builder().items(List.of()).truncated(true).build());

        assertThrows(StorageException.class,
                () -> StorageProviderSupport.copy(provider, CopyObjectRequest.builder()
                        .sourceObjectKey("files")
                        .targetObjectKey("archive")
                        .recursive(true).build()));
    }
}
