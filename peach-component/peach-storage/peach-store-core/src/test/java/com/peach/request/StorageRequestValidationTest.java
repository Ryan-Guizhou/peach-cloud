package com.peach.request;

import com.peach.content.UploadContent;
import com.peach.exception.StorageException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageRequestValidationTest {

    @Test
    void shouldRejectBlankObjectKey() {
        assertThrows(StorageException.class, () -> DownloadObjectRequest.builder().objectKey(" ").build());
        assertThrows(StorageException.class, () -> HeadObjectRequest.builder().objectKey("").build());
        assertThrows(StorageException.class, () -> UploadObjectRequest.builder()
                .objectKey(null)
                .content(UploadContent.of("demo"))
                .build());
    }

    @Test
    void shouldRejectMissingUploadContent() {
        assertThrows(StorageException.class, () -> UploadObjectRequest.builder()
                .objectKey("docs/a.txt")
                .build());
    }

    @Test
    void shouldRejectNonPositiveExpireSeconds() {
        assertThrows(StorageException.class, () -> PresignedUrlRequest.builder()
                .objectKey("docs/a.txt")
                .expireSeconds(0)
                .build());
    }

    @Test
    void shouldRejectInvalidListRequest() {
        assertThrows(StorageException.class, () -> ListObjectsRequest.builder().maxKeys(0).build());
        assertThrows(StorageException.class, () -> ListObjectsRequest.builder().delimiter(" ").build());
    }
}
