package com.peach.quickstart;

import com.peach.content.UploadContent;
import com.peach.enums.StorageContentType;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.HeadObjectRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.ObjectInfo;
import com.peach.storage.StorageTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基于 LOCAL Provider 验证上传、元数据与下载闭环。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.store.demo.enabled=false")
class StorageLocalScenarioTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("peach.storage.enabled", () -> "true");
        registry.add("peach.storage.primary", () -> "local");
        registry.add("peach.storage.providers.local.type", () -> "LOCAL");
        registry.add("peach.storage.providers.local.bucket-name", () -> "quickstart");
        registry.add("peach.storage.providers.local.root-path", () -> tempDir.toAbsolutePath().toString());
        registry.add("peach.storage.providers.local.domain", () -> "http://localhost/files");
    }

    @Autowired
    private StorageTemplate storageTemplate;

    @Test
    void shouldUploadHeadAndDownload() throws Exception {
        String objectKey = "quickstarts/it-hello.txt";
        String payload = "hello-local-provider";

        storageTemplate.upload(UploadObjectRequest.builder()
                .objectKey(objectKey)
                .content(UploadContent.of(payload, StandardCharsets.UTF_8))
                .contentType(StorageContentType.TEXT_PLAIN_UTF8)
                .build());

        ObjectInfo info = storageTemplate.head(HeadObjectRequest.builder().objectKey(objectKey).build());
        assertThat(info.getSize()).isEqualTo(payload.getBytes(StandardCharsets.UTF_8).length);

        try (InputStream in = storageTemplate.download(DownloadObjectRequest.builder().objectKey(objectKey).build())) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(text).isEqualTo(payload);
        }
    }
}
