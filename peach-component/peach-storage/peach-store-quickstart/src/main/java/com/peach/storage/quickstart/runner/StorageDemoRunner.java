package com.peach.storage.quickstart.runner;

import com.peach.response.DeleteResult;
import com.peach.response.ObjectInfo;
import com.peach.storage.quickstart.example.DeleteObjectExample;
import com.peach.storage.quickstart.example.HeadObjectExample;
import com.peach.storage.quickstart.example.UploadDownloadExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.nio.charset.StandardCharsets;

/**
 * 启动后演示 LOCAL provider 的 upload/download、head 与 delete 闭环。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.store.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageDemoRunner implements ApplicationRunner {

    static final String PUT_GET_KEY = "quickstart/demo/put-get.txt";
    static final String HEAD_KEY = "quickstart/demo/head.txt";
    static final String DELETE_KEY = "quickstart/demo/delete.txt";
    static final String PUT_GET_PAYLOAD = "peach-store-put-get";
    static final String HEAD_PAYLOAD = "peach-store-head";
    static final String DELETE_PAYLOAD = "peach-store-delete";

    private final UploadDownloadExample uploadDownloadExample;
    private final HeadObjectExample headObjectExample;
    private final DeleteObjectExample deleteObjectExample;

    @Override
    public void run(ApplicationArguments args) {
        runPutThenGet();
        runHead();
        runDelete();
        log.info("storage demo finished");
    }

    private void runPutThenGet() {
        log.info("=== StorageTemplate upload/download demo ===");
        String downloaded = uploadDownloadExample.putThenGet(PUT_GET_KEY, PUT_GET_PAYLOAD);
        if (!PUT_GET_PAYLOAD.equals(downloaded)) {
            throw new IllegalStateException("putThenGet demo failed");
        }
        log.info("putThenGet demo ok, objectKey={}", PUT_GET_KEY);
    }

    private void runHead() {
        log.info("=== StorageTemplate head demo ===");
        ObjectInfo info = headObjectExample.uploadThenHead(HEAD_KEY, HEAD_PAYLOAD);
        long expected = HEAD_PAYLOAD.getBytes(StandardCharsets.UTF_8).length;
        if (info.getSize() != expected) {
            throw new IllegalStateException("head demo failed");
        }
        log.info("head demo ok, objectKey={}, size={}", HEAD_KEY, info.getSize());
    }

    private void runDelete() {
        log.info("=== StorageTemplate delete demo ===");
        DeleteResult deleted = deleteObjectExample.uploadThenDelete(DELETE_KEY, DELETE_PAYLOAD);
        if (!deleted.isDeleted()) {
            throw new IllegalStateException("delete demo failed");
        }
        log.info("delete demo ok, objectKey={}", DELETE_KEY);
    }
}
