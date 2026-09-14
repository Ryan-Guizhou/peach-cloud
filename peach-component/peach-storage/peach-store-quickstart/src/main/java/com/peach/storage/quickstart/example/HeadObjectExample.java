package com.peach.storage.quickstart.example;

import com.peach.content.UploadContent;
import com.peach.enums.StorageContentType;
import com.peach.request.HeadObjectRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.ObjectInfo;
import com.peach.storage.StorageTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 注入 {@link StorageTemplate}，上传后调用 head 读取对象元信息。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class HeadObjectExample {

    private final StorageTemplate storageTemplate;

    /**
     * 写入对象后查询元信息，不下载正文。
     *
     * @param objectKey 业务对象 key
     * @param payload 演示文本，用于确定对象大小
     * @return 对象元信息
     */
    public ObjectInfo uploadThenHead(String objectKey, String payload) {
        storageTemplate.upload(UploadObjectRequest.builder()
                .objectKey(objectKey)
                .content(UploadContent.of(payload, StandardCharsets.UTF_8))
                .contentType(StorageContentType.TEXT_PLAIN_UTF8)
                .build());
        ObjectInfo info = storageTemplate.head(HeadObjectRequest.builder()
                .objectKey(objectKey)
                .build());
        log.info("head objectKey={}, size={}", info.getObjectKey(), info.getSize());
        return info;
    }
}
