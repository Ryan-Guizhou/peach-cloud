package com.peach.mongo.quickstart;

import com.github.pagehelper.PageInfo;
import com.peach.mongo.quickstart.example.AuditLogPageDeleteExample;
import com.peach.mongo.quickstart.example.AuditLogUpdateExample;
import com.peach.mongo.quickstart.example.AuditLogWriteQueryExample;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@code IMongoService} 的写入查询、更新、分页删除。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@SpringBootTest(properties = "quickstart.mongo.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class MongoCapabilityTest {

    @Container
    @SuppressWarnings("resource")
    static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("peach.mongo.uri", MONGO::getReplicaSetUrl);
        registry.add("peach.mongo.database", () -> "peach_qs_it");
    }

    @Autowired
    private AuditLogWriteQueryExample writeQueryExample;

    @Autowired
    private AuditLogUpdateExample updateExample;

    @Autowired
    private AuditLogPageDeleteExample pageDeleteExample;

    @BeforeEach
    void clean() {
        writeQueryExample.clear();
        updateExample.clear();
        pageDeleteExample.clear();
    }

    @Test
    void shouldInsertAndQueryAuditLog() {
        String id = writeQueryExample.insert("CREATE_ORDER", "it-write");
        assertThat(id).isNotBlank();

        Document found = writeQueryExample.findById(id);
        assertThat(found).isNotNull();
        assertThat(found.getString("action")).isEqualTo("CREATE_ORDER");
        assertThat(found.getString("actor")).isEqualTo("it-write");

        List<Document> matched = writeQueryExample.findByActor("it-write");
        assertThat(matched).hasSize(1);
        assertThat(matched.get(0).getString("action")).isEqualTo("CREATE_ORDER");
    }

    @Test
    void shouldUpdateAuditLogAction() {
        String id = updateExample.insert("CREATE_ORDER", "it-update");
        assertThat(updateExample.updateAction(id, "UPDATE_ORDER")).isTrue();

        Document found = updateExample.findById(id);
        assertThat(found).isNotNull();
        assertThat(found.getString("action")).isEqualTo("UPDATE_ORDER");
        assertThat(found.getString("actor")).isEqualTo("it-update");
    }

    @Test
    void shouldPageAndDeleteAuditLogs() {
        List<String> ids = pageDeleteExample.insertMany(
                List.of("PAGE_A", "PAGE_B", "PAGE_C"), "it-page");
        assertThat(ids).hasSize(3);
        assertThat(pageDeleteExample.countByActor("it-page")).isEqualTo(3);

        PageInfo<Document> page = pageDeleteExample.pageByActor("it-page", 1, 2);
        assertThat(page.getTotal()).isEqualTo(3);
        assertThat(page.getPages()).isEqualTo(2);
        assertThat(page.getList()).hasSize(2);
        assertThat(page.getList().get(0).getString("action")).isEqualTo("PAGE_A");
        assertThat(page.getList().get(1).getString("action")).isEqualTo("PAGE_B");

        assertThat(pageDeleteExample.deleteById(ids.get(0))).isTrue();
        assertThat(pageDeleteExample.findById(ids.get(0))).isNull();
        assertThat(pageDeleteExample.countByActor("it-page")).isEqualTo(2);
    }
}
