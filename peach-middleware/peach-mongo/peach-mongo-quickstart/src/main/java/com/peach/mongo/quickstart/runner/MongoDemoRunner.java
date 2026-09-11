package com.peach.mongo.quickstart.runner;

import com.peach.mongo.quickstart.scenario.AuditLogScenarioService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后演示审计日志 CRUD。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.mongo.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MongoDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoDemoRunner.class);

    private final AuditLogScenarioService scenarioService;

    /**
     * @param scenarioService 审计场景
     */
    public MongoDemoRunner(AuditLogScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String id = scenarioService.create("LOGIN", "demo-user");
        Document found = scenarioService.findById(id);
        boolean updated = scenarioService.updateAction(id, "LOGIN_OK");
        boolean deleted = scenarioService.delete(id);
        log.info("mongo quickstart finished, idPresent={}, foundAction={}, updated={}, deleted={}, remainCount={}",
                id != null,
                found == null ? null : found.getString("action"),
                updated,
                deleted,
                scenarioService.count());
    }
}
