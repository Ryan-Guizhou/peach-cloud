package com.peach.storage.quickstart;

import com.peach.response.DeleteResult;
import com.peach.response.ObjectInfo;
import com.peach.storage.StorageTemplate;
import com.peach.storage.quickstart.example.DeleteObjectExample;
import com.peach.storage.quickstart.example.HeadObjectExample;
import com.peach.storage.quickstart.example.UploadDownloadExample;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 LOCAL provider 经 {@link StorageTemplate} 完成 upload/download、head 与 delete。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@SpringBootTest(properties = "quickstart.store.demo.enabled=false")
class StorageCapabilityTest {

    private static final String PUT_GET_KEY = "quickstart/it/put-get.txt";
    private static final String HEAD_KEY = "quickstart/it/head.txt";
    private static final String DELETE_KEY = "quickstart/it/delete.txt";
    private static final String PUT_GET_PAYLOAD = "it-store-put-get";
    private static final String HEAD_PAYLOAD = "it-store-head";
    private static final String DELETE_PAYLOAD = "it-store-delete";

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

    @Autowired
    private UploadDownloadExample uploadDownloadExample;

    @Autowired
    private HeadObjectExample headObjectExample;

    @Autowired
    private DeleteObjectExample deleteObjectExample;

    @Test
    void shouldUploadThenDownloadSamePayload() {
        String downloaded = uploadDownloadExample.putThenGet(PUT_GET_KEY, PUT_GET_PAYLOAD);
        assertThat(downloaded).isEqualTo(PUT_GET_PAYLOAD);
        assertThat(storageTemplate.exists(PUT_GET_KEY)).isTrue();
    }

    @Test
    void shouldHeadUploadedObject() {
        ObjectInfo info = headObjectExample.uploadThenHead(HEAD_KEY, HEAD_PAYLOAD);
        assertThat(info.getObjectKey()).isEqualTo(HEAD_KEY);
        assertThat(info.getSize()).isEqualTo(HEAD_PAYLOAD.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void shouldDeleteAndProveGone() {
        DeleteResult deleted = deleteObjectExample.uploadThenDelete(DELETE_KEY, DELETE_PAYLOAD);
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(storageTemplate.exists(DELETE_KEY)).isFalse();
    }
}
