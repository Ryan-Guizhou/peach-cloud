package com.peach.storage.provider;

import com.peach.config.StorageProperties;
import com.peach.content.UploadContent;
import com.peach.request.BatchDeleteObjectsRequest;
import com.peach.request.CopyObjectRequest;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.HeadObjectRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.MoveObjectRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.BatchDeleteResult;
import com.peach.response.CopyResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.MoveResult;
import com.peach.response.ObjectInfo;
import com.peach.response.UploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadDownloadHeadDeleteAndListWithPagination() throws Exception {
        LocalFileStorageProvider provider = new LocalFileStorageProvider(localConfig());

        UploadResult first = provider.upload(UploadObjectRequest.builder()
                .objectKey("docs/a.txt")
                .content(UploadContent.of("hello-a", StandardCharsets.UTF_8))
                .build());
        provider.upload(UploadObjectRequest.builder()
                .objectKey("docs/b.txt")
                .content(UploadContent.of("hello-b", StandardCharsets.UTF_8))
                .build());

        ObjectInfo head = provider.head(HeadObjectRequest.builder().objectKey("docs/a.txt").build());
        assertEquals("docs/a.txt", head.getObjectKey());
        assertTrue(head.getSize() > 0);

        try (InputStream inputStream = provider.download(DownloadObjectRequest.builder().objectKey("docs/a.txt").build())) {
            assertEquals("hello-a", readText(inputStream));
        }

        ListObjectsResult firstPage = provider.list(ListObjectsRequest.builder()
                .prefix("docs")
                .maxKeys(1)
                .build());
        assertEquals(1, firstPage.getItems().size());
        assertTrue(firstPage.isTruncated());
        assertNotNull(firstPage.getNextContinuationToken());
        assertEquals("docs/a.txt", firstPage.getItems().get(0).getObjectKey());

        ListObjectsResult secondPage = provider.list(ListObjectsRequest.builder()
                .prefix("docs")
                .maxKeys(1)
                .continuationToken(firstPage.getNextContinuationToken())
                .build());
        assertEquals(1, secondPage.getItems().size());
        assertFalse(secondPage.isTruncated());
        assertEquals("docs/b.txt", secondPage.getItems().get(0).getObjectKey());

        provider.delete(DeleteObjectRequest.builder().objectKey("docs/a.txt").build());
        assertFalse(provider.exists("docs/a.txt"));
        assertEquals("docs/a.txt", first.getObjectKey());
    }

    @Test
    void shouldSupportBatchDeleteCopyAndMove() throws Exception {
        LocalFileStorageProvider provider = new LocalFileStorageProvider(localConfig());

        provider.upload(UploadObjectRequest.builder()
                .objectKey("docs/source.txt")
                .content(UploadContent.of("copy-me", StandardCharsets.UTF_8))
                .build());
        provider.upload(UploadObjectRequest.builder()
                .objectKey("docs/delete-a.txt")
                .content(UploadContent.of("a", StandardCharsets.UTF_8))
                .build());
        provider.upload(UploadObjectRequest.builder()
                .objectKey("docs/delete-b.txt")
                .content(UploadContent.of("b", StandardCharsets.UTF_8))
                .build());

        CopyResult copyResult = provider.copy(CopyObjectRequest.builder()
                .sourceObjectKey("docs/source.txt")
                .targetObjectKey("archive/copied.txt")
                .build());
        assertTrue(copyResult.isCopied());
        assertTrue(provider.exists("archive/copied.txt"));

        MoveResult moveResult = provider.move(MoveObjectRequest.builder()
                .sourceObjectKey("archive/copied.txt")
                .targetObjectKey("archive/moved.txt")
                .build());
        assertTrue(moveResult.isMoved());
        assertFalse(provider.exists("archive/copied.txt"));
        assertTrue(provider.exists("archive/moved.txt"));

        BatchDeleteResult batchDeleteResult = provider.batchDelete(BatchDeleteObjectsRequest.builder()
                .addObjectKey("docs/delete-a.txt")
                .addObjectKey("docs/delete-b.txt")
                .build());
        assertEquals(2, batchDeleteResult.getDeletedCount());
        assertFalse(provider.exists("docs/delete-a.txt"));
        assertFalse(provider.exists("docs/delete-b.txt"));
    }

    @Test
    void shouldTreatBucketNameAsAliasForBucketlessProvider() throws Exception {
        LocalFileStorageProvider provider = new LocalFileStorageProvider(localConfig());

        UploadResult result = provider.upload(UploadObjectRequest.builder()
                .bucketName("bucket")
                .objectKey("docs/alias.txt")
                .content(UploadContent.of("alias", StandardCharsets.UTF_8))
                .build());
        assertEquals("bucket", result.getBucketName());

        ObjectInfo head = provider.head(HeadObjectRequest.builder()
                .bucketName("bucket")
                .objectKey("docs/alias.txt")
                .build());
        assertEquals("bucket", head.getBucketName());

        assertThrows(com.peach.exception.StorageException.class, () -> provider.head(HeadObjectRequest.builder()
                .bucketName("another-bucket")
                .objectKey("docs/alias.txt")
                .build()));
    }

    private StorageProperties.StorageProvider localConfig() {
        StorageProperties.StorageProvider provider = new StorageProperties.StorageProvider();
        provider.setName("local");
        provider.setType(com.peach.enums.StorageType.LOCAL);
        provider.setBucketName("bucket");
        provider.setRootPath(tempDir.toString());
        provider.setPrefix("tenant-a");
        provider.setDomain("http://localhost/files");
        provider.setExtraProperties(Collections.<String, String>emptyMap());
        return provider;
    }

    private String readText(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
}
