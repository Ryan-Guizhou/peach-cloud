package com.peach.request;

import com.peach.content.UploadContent;
import com.peach.exception.StorageException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 存储请求ValidationTest。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

class StorageRequestValidationTest {

    @Test
    void shouldRejectBlankObjectKey() {
        assertThrows(StorageException.class, () -> buildDownloadWithBlankKey());
        assertThrows(StorageException.class, () -> buildHeadWithEmptyKey());
        assertThrows(StorageException.class, () -> buildUploadWithNullKey());
    }

    @Test
    void shouldRejectMissingUploadContent() {
        assertThrows(StorageException.class, () -> buildUploadWithoutContent());
    }

    @Test
    void shouldRejectNonPositiveExpireSeconds() {
        assertThrows(StorageException.class, () -> buildPresignedWithZeroExpire());
    }

    @Test
    void shouldRejectInvalidListRequest() {
        assertThrows(StorageException.class, () -> buildListWithZeroMaxKeys());
        assertThrows(StorageException.class, () -> buildListWithBlankDelimiter());
    }

    private static void buildDownloadWithBlankKey() {
        DownloadObjectRequest.builder().objectKey(" ").build();
    }

    private static void buildHeadWithEmptyKey() {
        HeadObjectRequest.builder().objectKey("").build();
    }

    private static void buildUploadWithNullKey() {
        UploadObjectRequest.builder()
                .objectKey(null)
                .content(UploadContent.of("demo"))
                .build();
    }

    private static void buildUploadWithoutContent() {
        UploadObjectRequest.builder()
                .objectKey("docs/a.txt")
                .build();
    }

    private static void buildPresignedWithZeroExpire() {
        PresignedUrlRequest.builder()
                .objectKey("docs/a.txt")
                .expireSeconds(0)
                .build();
    }

    private static void buildListWithZeroMaxKeys() {
        ListObjectsRequest.builder().maxKeys(0).build();
    }

    private static void buildListWithBlankDelimiter() {
        ListObjectsRequest.builder().delimiter(" ").build();
    }
}
