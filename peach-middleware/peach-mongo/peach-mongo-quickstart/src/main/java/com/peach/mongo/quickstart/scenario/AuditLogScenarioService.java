package com.peach.mongo.quickstart.scenario;

import com.peach.mongo.IMongoService;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 审计日志 CRUD 场景。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class AuditLogScenarioService {

    public static final String COLLECTION = "peach_qs_audit_log";

    private final IMongoService<?> mongoService;

    /**
     * @param mongoService Mongo 统一访问入口
     */
    public AuditLogScenarioService(IMongoService<?> mongoService) {
        this.mongoService = mongoService;
    }

    /**
     * 写入审计日志。
     *
     * @param action 动作
     * @param actor  操作者标识（非敏感）
     * @return 文档 id
     */
    public String create(String action, String actor) {
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
     * 按 id 查询。
     *
     * @param id 文档 id
     * @return 文档，不存在时返回 null
     */
    public Document findById(String id) {
        List<Document> list = mongoService.findList(COLLECTION, new Document("_id", new ObjectId(id)));
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 更新动作字段。
     *
     * @param id     文档 id
     * @param action 新动作
     * @return 是否更新成功
     */
    public boolean updateAction(String id, String action) {
        return mongoService.updateOne(
                COLLECTION,
                new Document("_id", new ObjectId(id)),
                new Document("$set", new Document("action", action)));
    }

    /**
     * 删除审计日志。
     *
     * @param id 文档 id
     * @return 是否删除成功
     */
    public boolean delete(String id) {
        return mongoService.deleteOne(COLLECTION, new Document("_id", new ObjectId(id)));
    }

    /**
     * 统计集合文档数。
     *
     * @return 数量
     */
    public long count() {
        return mongoService.count(COLLECTION);
    }
}
