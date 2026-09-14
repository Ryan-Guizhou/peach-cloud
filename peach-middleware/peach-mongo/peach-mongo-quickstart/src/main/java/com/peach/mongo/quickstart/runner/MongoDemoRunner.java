package com.peach.mongo.quickstart.runner;

import com.github.pagehelper.PageInfo;
import com.peach.mongo.quickstart.example.AuditLogPageDeleteExample;
import com.peach.mongo.quickstart.example.AuditLogUpdateExample;
import com.peach.mongo.quickstart.example.AuditLogWriteQueryExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 启动后演示 {@code IMongoService} 的写入查询、更新、分页删除。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.mongo.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MongoDemoRunner implements ApplicationRunner {

    private static final String WRITE_ACTOR = "qs-write";
    private static final String UPDATE_ACTOR = "qs-update";
    private static final String PAGE_ACTOR = "qs-page";

    private final AuditLogWriteQueryExample writeQueryExample;
    private final AuditLogUpdateExample updateExample;
    private final AuditLogPageDeleteExample pageDeleteExample;

    @Override
    public void run(ApplicationArguments args) {
        runWriteQueryDemo();
        runUpdateDemo();
        runPageDeleteDemo();
        log.info("mongo demo finished");
    }

    private void runWriteQueryDemo() {
        log.info("=== IMongoService insert / find demo ===");
        writeQueryExample.clear();
        String id = writeQueryExample.insert("QS_INSERT", WRITE_ACTOR);
        Document found = writeQueryExample.findById(id);
        List<Document> matched = writeQueryExample.findByActor(WRITE_ACTOR);
        log.info("writeQuery idPresent={}, action={}, matched={}",
                id != null, found == null ? null : found.getString("action"), matched.size());
        if (id == null || found == null || !"QS_INSERT".equals(found.getString("action")) || matched.isEmpty()) {
            throw new IllegalStateException("write query demo failed");
        }
        writeQueryExample.clear();
    }

    private void runUpdateDemo() {
        log.info("=== IMongoService updateOne demo ===");
        updateExample.clear();
        String id = updateExample.insert("QS_UPDATE_SRC", UPDATE_ACTOR);
        boolean updated = updateExample.updateAction(id, "QS_UPDATE_DST");
        Document found = updateExample.findById(id);
        log.info("update idPresent={}, updated={}, action={}",
                id != null, updated, found == null ? null : found.getString("action"));
        if (!updated || found == null || !"QS_UPDATE_DST".equals(found.getString("action"))) {
            throw new IllegalStateException("update demo failed");
        }
        updateExample.clear();
    }

    private void runPageDeleteDemo() {
        log.info("=== IMongoService findPage / deleteOne demo ===");
        pageDeleteExample.clear();
        List<String> ids = pageDeleteExample.insertMany(List.of("QS_PAGE_A", "QS_PAGE_B", "QS_PAGE_C"), PAGE_ACTOR);
        PageInfo<Document> page = pageDeleteExample.pageByActor(PAGE_ACTOR, 1, 2);
        long beforeDelete = pageDeleteExample.countByActor(PAGE_ACTOR);
        boolean deleted = pageDeleteExample.deleteById(ids.get(0));
        Document afterDelete = pageDeleteExample.findById(ids.get(0));
        long afterCount = pageDeleteExample.countByActor(PAGE_ACTOR);
        log.info("pageDelete total={}, pageSize={}, deleted={}, afterNull={}, afterCount={}",
                page.getTotal(), page.getList() == null ? 0 : page.getList().size(), deleted, afterDelete == null, afterCount);
        if (ids.size() != 3 || page.getTotal() != 3 || page.getList() == null || page.getList().size() != 2
                || beforeDelete != 3 || !deleted || afterDelete != null || afterCount != 2) {
            throw new IllegalStateException("page delete demo failed");
        }
        pageDeleteExample.clear();
    }
}
