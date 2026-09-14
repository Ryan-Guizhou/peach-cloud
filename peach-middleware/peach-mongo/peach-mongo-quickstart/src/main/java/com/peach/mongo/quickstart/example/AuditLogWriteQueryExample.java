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
 * 审计日志写入与查询：演示 {@link IMongoService#insertOne(String, Document)} 与
 * {@link IMongoService#findList(String, Document)}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class AuditLogWriteQueryExample {

    public static final String COLLECTION = "peach_qs_audit_write";

    private final IMongoService<?> mongoService;

    /**
     * 写入一条审计日志，返回驱动回填的文档 id。
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
        String hexId = id == null ? null : id.toHexString();
        log.info("audit insert, actor={}, action={}, idPresent={}", actor, action, hexId != null);
        return hexId;
    }

    /**
     * 按文档 id 查询。
     *
     * @param id 文档 id
     * @return 文档，不存在时返回 {@code null}
     */
    public Document findById(String id) {
        List<Document> list = mongoService.findList(COLLECTION, new Document("_id", new ObjectId(id)));
        Document found = list == null || list.isEmpty() ? null : list.get(0);
        log.info("audit findById, hit={}", found != null);
        return found;
    }

    /**
     * 按操作者条件查询。
     *
     * @param actor 操作者标识
     * @return 匹配文档，不会返回 {@code null}
     */
    public List<Document> findByActor(String actor) {
        List<Document> list = mongoService.findList(COLLECTION, new Document("actor", actor));
        List<Document> matched = list == null ? List.of() : list;
        log.info("audit findByActor, size={}", matched.size());
        return matched;
    }

    /**
     * 清空演示集合，避免样例数据互相干扰。
     */
    public void clear() {
        mongoService.deleteMany(COLLECTION, new Document());
    }
}
