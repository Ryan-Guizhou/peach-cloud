package com.peach.example;

import org.springframework.stereotype.Indexed;
import com.alibaba.fastjson.JSON;
import com.peach.content.UploadContent;
import com.peach.enums.StorageCapability;
import com.peach.enums.StorageContentType;
import com.peach.request.AbortMultipartUploadRequest;
import com.peach.request.BatchDeleteObjectsRequest;
import com.peach.request.CopyObjectRequest;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.FrontendUploadTokenRequest;
import com.peach.request.HeadObjectRequest;
import com.peach.request.InitiateMultipartUploadRequest;
import com.peach.request.ListObjectsRequest;
import com.peach.request.MoveObjectRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.request.UploadPartRequest;
import com.peach.response.AbortMultipartUploadResult;
import com.peach.response.BatchDeleteResult;
import com.peach.response.CopyResult;
import com.peach.response.FrontendUploadTokenResult;
import com.peach.response.InitiateMultipartUploadResult;
import com.peach.response.ListObjectsResult;
import com.peach.response.MoveResult;
import com.peach.response.ObjectInfo;
import com.peach.response.UploadPartResult;
import com.peach.response.UploadResult;
import com.peach.storage.StorageTemplate;
import com.peach.storage.spi.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 存储Example运行器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Indexed
@Component
public class StorageExampleRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageExampleRunner.class);

    private static final String OBJECT_KEY = "examples/hello.txt";
    private static final String COPY_KEY = "examples/copy/hello-copy.txt";
    private static final String MOVE_SOURCE_KEY = "examples/move/hello-move-source.txt";
    private static final String MOVE_TARGET_KEY = "examples/move/hello-move-target.txt";
    private static final String BATCH_KEY_A = "examples/batch/a.txt";
    private static final String BATCH_KEY_B = "examples/batch/b.txt";
    private static final String MULTIPART_KEY = "examples/multipart/demo.bin";

    private final StorageTemplate storageTemplate;

    public StorageExampleRunner(StorageTemplate storageTemplate) {
        this.storageTemplate = storageTemplate;
    }

    @Override
    public void run(String... args) {
        StorageProvider provider = storageTemplate.primary();

        UploadResult uploadResult = storageTemplate.upload(UploadObjectRequest.builder()
                .objectKey(OBJECT_KEY)
                .content(UploadContent.of("hello peach store starter", StandardCharsets.UTF_8))
                .contentType(StorageContentType.TEXT_PLAIN_UTF8)
                .build());

        ObjectInfo objectInfo = storageTemplate.head(HeadObjectRequest.builder()
                .objectKey(OBJECT_KEY)
                .build());

        String text;
        try (InputStream inputStream = storageTemplate.download(DownloadObjectRequest.builder()
                .objectKey(OBJECT_KEY)
                .build())) {
            text = readText(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to download example object", ex);
        }

        ListObjectsResult objects = storageTemplate.list(ListObjectsRequest.builder()
                .prefix("examples")
                .maxKeys(10)
                .build());

        String presignedUrl = storageTemplate.generatePresignedUrl(OBJECT_KEY, 1800);
        log.info("presignedUrl={}", presignedUrl);
        if (log.isInfoEnabled()) { log.info("head={}", JSON.toJSONString(objectInfo)); }

        log.info("Peach store example finished.");
        log.info("provider={}", uploadResult.getProviderName());
        log.info("bucket={}", uploadResult.getBucketName());
        log.info("objectKey={}", uploadResult.getObjectKey());
        log.info("size={}", objectInfo.getSize());
        log.info("downloadText={}", text);
        log.info("listCount={}", objects.getItems().size());

        if (provider.supports(StorageCapability.COPY)) {
            runCopyExample();
        }
        if (provider.supports(StorageCapability.MOVE)) {
            runMoveExample();
        }
        if (provider.supports(StorageCapability.BATCH_DELETE)) {
            runBatchDeleteExample();
        }
        if (provider.supports(StorageCapability.FRONTEND_UPLOAD_TOKEN)
                || provider.supports(StorageCapability.MULTIPART_UPLOAD)) {
            runOssFrontendExamples(provider);
        }
    }

    private void runCopyExample() {
        CopyResult copyResult = storageTemplate.copy(CopyObjectRequest.builder()
                .sourceObjectKey(OBJECT_KEY)
                .targetObjectKey(COPY_KEY)
                .overwrite(true)
                .build());
        if (log.isInfoEnabled()) { log.info("copyResult={}", JSON.toJSONString(copyResult)); }
    }

    private void runMoveExample() {
        storageTemplate.upload(UploadObjectRequest.builder()
                .objectKey(MOVE_SOURCE_KEY)
                .content(UploadContent.of("move example", StandardCharsets.UTF_8))
                .contentType(StorageContentType.TEXT_PLAIN_UTF8)
                .build());

        MoveResult moveResult = storageTemplate.move(MoveObjectRequest.builder()
                .sourceObjectKey(MOVE_SOURCE_KEY)
                .targetObjectKey(MOVE_TARGET_KEY)
                .overwrite(true)
                .build());
        if (log.isInfoEnabled()) { log.info("moveResult={}", JSON.toJSONString(moveResult)); }
    }

    private void runBatchDeleteExample() {
        storageTemplate.upload(UploadObjectRequest.builder()
                .objectKey(BATCH_KEY_A)
                .content(UploadContent.of("batch-a", StandardCharsets.UTF_8))
                .contentType(StorageContentType.TEXT_PLAIN_UTF8)
                .build());
        storageTemplate.upload(UploadObjectRequest.builder()
                .objectKey(BATCH_KEY_B)
                .content(UploadContent.of("batch-b", StandardCharsets.UTF_8))
                .contentType(StorageContentType.TEXT_PLAIN_UTF8)
                .build());

        BatchDeleteResult batchDeleteResult = storageTemplate.batchDelete(BatchDeleteObjectsRequest.builder()
                .objectKeys(Arrays.asList(BATCH_KEY_A, BATCH_KEY_B))
                .build());
        if (log.isInfoEnabled()) { log.info("batchDeleteResult={}", JSON.toJSONString(batchDeleteResult)); }
    }

    private void runOssFrontendExamples(StorageProvider provider) {
        if (provider.supports(StorageCapability.FRONTEND_UPLOAD_TOKEN)) {
            FrontendUploadTokenResult tokenResult = storageTemplate.createFrontendUploadToken(
                    FrontendUploadTokenRequest.builder()
                            .objectKey("examples/frontend/token-demo.txt")
                            .expireSeconds(300)
                            .maxSize(10 * 1024 * 1024L)
                            .build());
            if (log.isInfoEnabled()) { log.info("frontendUploadToken={}", JSON.toJSONString(tokenResult)); }
        }

        if (provider.supports(StorageCapability.MULTIPART_UPLOAD)) {
            InitiateMultipartUploadResult initResult = storageTemplate.initiateMultipartUpload(
                    InitiateMultipartUploadRequest.builder()
                            .objectKey(MULTIPART_KEY)
                            .contentType("application/octet-stream")
                            .build());
            if (log.isInfoEnabled()) { log.info("multipartInit={}", JSON.toJSONString(initResult)); }

            UploadPartResult uploadPartResult = storageTemplate.prepareUploadPart(
                    UploadPartRequest.builder()
                            .objectKey(MULTIPART_KEY)
                            .uploadId(initResult.getUploadId())
                            .partNumber(1)
                            .expireSeconds(900)
                            .build());
            if (log.isInfoEnabled()) { log.info("multipartPartUrl={}", JSON.toJSONString(uploadPartResult)); }

            AbortMultipartUploadResult abortResult = storageTemplate.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                            .objectKey(MULTIPART_KEY)
                            .uploadId(initResult.getUploadId())
                            .build());
            if (log.isInfoEnabled()) { log.info("multipartAbort={}", JSON.toJSONString(abortResult)); }
        }
    }

    private String readText(InputStream inputStream) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read storage object stream", ex);
        }
    }
}
