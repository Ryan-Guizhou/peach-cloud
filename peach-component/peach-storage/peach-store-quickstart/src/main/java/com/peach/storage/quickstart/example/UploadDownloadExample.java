package com.peach.storage.quickstart.example;

import com.peach.content.UploadContent;
import com.peach.enums.StorageContentType;
import com.peach.request.DownloadObjectRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.UploadResult;
import com.peach.storage.StorageTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 注入 {@link StorageTemplate}，用 LOCAL provider 完成 upload 后再 download，校验内容一致。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class UploadDownloadExample {

    private final StorageTemplate storageTemplate;

    /**
     * 写入对象后立即读取，返回下载到的文本。
     *
     * @param objectKey 业务对象 key，不得包含本地绝对路径或 {@code ..}
     * @param payload 演示文本，不得包含敏感信息
     * @return 下载内容
     */
    public String putThenGet(String objectKey, String payload) {
        UploadResult uploaded = storageTemplate.upload(UploadObjectRequest.builder()
                .objectKey(objectKey)
                .content(UploadContent.of(payload, StandardCharsets.UTF_8))
                .contentType(StorageContentType.TEXT_PLAIN_UTF8)
                .build());
        String downloaded = readText(objectKey);
        log.info("putThenGet provider={}, objectKey={}, downloadedLength={}",
                uploaded.getProviderName(), uploaded.getObjectKey(), downloaded.length());
        return downloaded;
    }

    private String readText(String objectKey) {
        try (InputStream in = storageTemplate.download(DownloadObjectRequest.builder()
                .objectKey(objectKey)
                .build())) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("download failed, objectKey=" + objectKey, ex);
        }
    }
}
