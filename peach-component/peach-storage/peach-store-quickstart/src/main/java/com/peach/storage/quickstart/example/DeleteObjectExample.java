package com.peach.storage.quickstart.example;

import com.peach.content.UploadContent;
import com.peach.enums.StorageContentType;
import com.peach.request.DeleteObjectRequest;
import com.peach.request.UploadObjectRequest;
import com.peach.response.DeleteResult;
import com.peach.storage.StorageTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 注入 {@link StorageTemplate}，上传后删除对象，并用 {@code exists} 证明对象已不存在。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class DeleteObjectExample {

    private final StorageTemplate storageTemplate;

    /**
     * 先写入再删除，删除后对象必须不存在。
     *
     * @param objectKey 业务对象 key
     * @param payload 演示文本
     * @return 删除结果
     */
    public DeleteResult uploadThenDelete(String objectKey, String payload) {
        storageTemplate.upload(UploadObjectRequest.builder()
                .objectKey(objectKey)
                .content(UploadContent.of(payload, StandardCharsets.UTF_8))
                .contentType(StorageContentType.TEXT_PLAIN_UTF8)
                .build());
        DeleteResult deleted = storageTemplate.delete(DeleteObjectRequest.builder()
                .objectKey(objectKey)
                .build());
        boolean existsAfter = storageTemplate.exists(objectKey);
        log.info("delete objectKey={}, deleted={}, existsAfter={}",
                deleted.getObjectKey(), deleted.isDeleted(), existsAfter);
        if (existsAfter) {
            throw new IllegalStateException("object still exists after delete: " + objectKey);
        }
        return deleted;
    }
}
