package com.peach.mongo.quickstart.example;

import com.peach.mongo.IMongoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 审计日志更新：演示 {@link IMongoService#updateOne(String, Document, Document)}，
 * 更新文档必须使用 {@code $set} 等操作符。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class AuditLogUpdateExample {

    public static final String COLLECTION = "peach_qs_audit_update";

    private final IMongoService<?> mongoService;

    /**
     * 写入待更新的审计日志。
     *
     * @param action 动作标识（非敏感）
     * @param actor  操作者标识（非敏感）
     * @return 文档 id
     */
    public String insert(String action, String actor) {
        Document document = new Document();
        document.put("action", action);
        document.put("actor", actor);
        document.put("createdAt", Instant.now().toString());
        boolean inserted = mongoService.insertOne(COLLECTION, document);
        if (!inserted) {
            throw new IllegalStateException("insert audit log failed");
        }
        ObjectId id = document.getObjectId("_id");
        return id == null ? null : id.toHexString();
    }

    /**
     * 按 id 更新 action 字段。
     *
     * @param id     文档 id
     * @param action 新动作
     * @return 是否调用成功（驱动未抛异常）
     */
    public boolean updateAction(String id, String action) {
        boolean updated = mongoService.updateOne(
                COLLECTION,
                new Document("_id", new ObjectId(id)),
                new Document("$set", new Document("action", action)));
        log.info("audit update, idPresent={}, updated={}", id != null, updated);
        return updated;
    }

    /**
     * 按文档 id 查询，用于核对更新结果。
     *
     * @param id 文档 id
     * @return 文档，不存在时返回 {@code null}
     */
    public Document findById(String id) {
        List<Document> list = mongoService.findList(COLLECTION, new Document("_id", new ObjectId(id)));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /**
     * 清空演示集合，避免样例数据互相干扰。
     */
    public void clear() {
        mongoService.deleteMany(COLLECTION, new Document());
    }
}
