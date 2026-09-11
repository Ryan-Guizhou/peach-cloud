package com.peach.mongo.quickstart.example;

import com.github.pagehelper.PageInfo;
import com.peach.mongo.IMongoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 审计日志分页与删除：演示 {@link IMongoService#insertMany(String, List)}、
 * {@link IMongoService#findPage(String, Document, Document, Integer, Integer)}、
 * {@link IMongoService#count(String, Document)} 与 {@link IMongoService#deleteOne(String, Document)}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class AuditLogPageDeleteExample {

    public static final String COLLECTION = "peach_qs_audit_page";

    private final IMongoService<?> mongoService;

    /**
     * 批量写入同一操作者的审计日志。
     *
     * @param actions 动作标识列表（非敏感）
     * @param actor   操作者标识（非敏感）
     * @return 写入文档 id，顺序与 {@code actions} 一致
     */
    public List<String> insertMany(List<String> actions, String actor) {
        List<Document> documents = new ArrayList<>(actions.size());
        Instant now = Instant.now();
        for (int i = 0; i < actions.size(); i++) {
            Document document = new Document();
            document.put("action", actions.get(i));
            document.put("actor", actor);
            document.put("createdAt", now.plusSeconds(i).toString());
            documents.add(document);
        }
        boolean inserted = mongoService.insertMany(COLLECTION, documents);
        if (!inserted) {
            throw new IllegalStateException("insertMany audit log failed");
        }
        List<String> ids = new ArrayList<>(documents.size());
        for (Document document : documents) {
            ObjectId id = document.getObjectId("_id");
            ids.add(id == null ? null : id.toHexString());
        }
        log.info("audit insertMany, actor={}, size={}", actor, ids.size());
        return ids;
    }

    /**
     * 按操作者分页，按 {@code createdAt} 升序。
     *
     * @param actor    操作者标识
     * @param pageNum  页码，从 1 开始
     * @param pageSize 页大小
     * @return 分页结果
     */
    public PageInfo<Document> pageByActor(String actor, int pageNum, int pageSize) {
        PageInfo<Document> page = mongoService.findPage(
                COLLECTION,
                new Document("actor", actor),
                new Document("createdAt", 1),
                pageNum,
                pageSize);
        log.info("audit page, actor={}, pageNum={}, pageSize={}, total={}, listSize={}",
                actor, pageNum, pageSize, page.getTotal(), page.getList() == null ? 0 : page.getList().size());
        return page;
    }

    /**
     * 按操作者统计文档数。
     *
     * @param actor 操作者标识
     * @return 文档数量
     */
    public long countByActor(String actor) {
        long count = mongoService.count(COLLECTION, new Document("actor", actor));
        log.info("audit count, actor={}, count={}", actor, count);
        return count;
    }

    /**
     * 按文档 id 删除。
     *
     * @param id 文档 id
     * @return 是否调用成功（驱动未抛异常）
     */
    public boolean deleteById(String id) {
        boolean deleted = mongoService.deleteOne(COLLECTION, new Document("_id", new ObjectId(id)));
        log.info("audit deleteById, idPresent={}, deleted={}", id != null, deleted);
        return deleted;
    }

    /**
     * 按文档 id 查询，用于核对删除结果。
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
