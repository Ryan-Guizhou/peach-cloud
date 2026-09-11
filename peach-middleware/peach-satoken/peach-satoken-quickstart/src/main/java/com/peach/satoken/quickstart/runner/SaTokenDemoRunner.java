package com.peach.satoken.quickstart.runner;

import com.peach.satoken.quickstart.scenario.AuthScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 启动后演示一次登录与鉴权。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.satoken.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SaTokenDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SaTokenDemoRunner.class);

    private final AuthScenarioService scenarioService;
    private final String demoUsername;
    private final String demoPassword;

    /**
     * @param scenarioService 鉴权场景
     * @param demoUsername    开发占位用户名
     * @param demoPassword    开发占位密码
     */
    public SaTokenDemoRunner(AuthScenarioService scenarioService,
                             @Value("${quickstart.satoken.demo.username}") String demoUsername,
                             @Value("${quickstart.satoken.demo.password}") String demoPassword) {
        this.scenarioService = scenarioService;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Object> login = scenarioService.login(demoUsername, demoPassword);
        Map<String, Object> profile = scenarioService.currentProfile();
        scenarioService.logout();
        log.info("satoken quickstart finished, loginId={}, tokenNamePresent={}, profileLogin={}",
                login.get("loginId"),
                login.get("tokenName") != null,
                profile.get("login"));
    }
}
