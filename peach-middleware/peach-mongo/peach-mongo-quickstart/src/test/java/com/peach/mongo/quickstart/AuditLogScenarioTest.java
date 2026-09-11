package com.peach.mongo.quickstart;

import com.peach.mongo.quickstart.scenario.AuditLogScenarioService;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基于 Testcontainers Mongo 验证审计日志 CRUD。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.mongo.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class AuditLogScenarioTest {

    @Container
    @SuppressWarnings("resource")
    static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("peach.mongo.uri", MONGO::getReplicaSetUrl);
        registry.add("peach.mongo.database", () -> "peach_qs_it");
    }

    @Autowired
    private AuditLogScenarioService scenarioService;

    @Test
    void shouldSupportAuditLogCrud() {
        String id = scenarioService.create("CREATE_ORDER", "it-user");
        assertThat(id).isNotBlank();

        Document found = scenarioService.findById(id);
        assertThat(found).isNotNull();
        assertThat(found.getString("action")).isEqualTo("CREATE_ORDER");
        assertThat(found.getString("actor")).isEqualTo("it-user");

        assertThat(scenarioService.updateAction(id, "UPDATE_ORDER")).isTrue();
        assertThat(scenarioService.findById(id).getString("action")).isEqualTo("UPDATE_ORDER");

        assertThat(scenarioService.delete(id)).isTrue();
        assertThat(scenarioService.findById(id)).isNull();
    }
}
