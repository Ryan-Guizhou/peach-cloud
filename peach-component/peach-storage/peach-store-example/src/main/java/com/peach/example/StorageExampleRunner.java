package com.peach.example;

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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class StorageExampleRunner implements CommandLineRunner {

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
    public void run(String... args) throws Exception {
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
        }

        ListObjectsResult objects = storageTemplate.list(ListObjectsRequest.builder()
                .prefix("examples")
                .maxKeys(10)
                .build());

        String presignedUrl = storageTemplate.generatePresignedUrl(OBJECT_KEY, 1800);
        System.out.println("presignedUrl= " + presignedUrl);
        System.out.println("head = " + JSON.toJSONString(objectInfo));

        System.out.println("Peach store example finished.");
        System.out.println("provider=" + uploadResult.getProviderName());
        System.out.println("bucket=" + uploadResult.getBucketName());
        System.out.println("objectKey=" + uploadResult.getObjectKey());
        System.out.println("size=" + objectInfo.getSize());
        System.out.println("downloadText=" + text);
        System.out.println("listCount=" + objects.getItems().size());

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
        System.out.println("copyResult=" + JSON.toJSONString(copyResult));
    }

    private void runMoveExample() throws Exception {
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
        System.out.println("moveResult=" + JSON.toJSONString(moveResult));
    }

    private void runBatchDeleteExample() throws Exception {
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
        System.out.println("batchDeleteResult=" + JSON.toJSONString(batchDeleteResult));
    }

    private void runOssFrontendExamples(StorageProvider provider) {
        if (provider.supports(StorageCapability.FRONTEND_UPLOAD_TOKEN)) {
            FrontendUploadTokenResult tokenResult = storageTemplate.createFrontendUploadToken(
                    FrontendUploadTokenRequest.builder()
                            .objectKey("examples/frontend/token-demo.txt")
                            .expireSeconds(300)
                            .maxSize(10 * 1024 * 1024L)
                            .build());
            System.out.println("frontendUploadToken=" + JSON.toJSONString(tokenResult));
        }

        if (provider.supports(StorageCapability.MULTIPART_UPLOAD)) {
            InitiateMultipartUploadResult initResult = storageTemplate.initiateMultipartUpload(
                    InitiateMultipartUploadRequest.builder()
                            .objectKey(MULTIPART_KEY)
                            .contentType("application/octet-stream")
                            .build());
            System.out.println("multipartInit=" + JSON.toJSONString(initResult));

            UploadPartResult uploadPartResult = storageTemplate.prepareUploadPart(
                    UploadPartRequest.builder()
                            .objectKey(MULTIPART_KEY)
                            .uploadId(initResult.getUploadId())
                            .partNumber(1)
                            .expireSeconds(900)
                            .build());
            System.out.println("multipartPartUrl=" + JSON.toJSONString(uploadPartResult));

            AbortMultipartUploadResult abortResult = storageTemplate.abortMultipartUpload(
                    AbortMultipartUploadRequest.builder()
                            .objectKey(MULTIPART_KEY)
                            .uploadId(initResult.getUploadId())
                            .build());
            System.out.println("multipartAbort=" + JSON.toJSONString(abortResult));
        }
    }

    private String readText(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}