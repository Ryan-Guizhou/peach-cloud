package com.peach.email.quickstart.runner;

import com.peach.email.core.SendResult;
import com.peach.email.quickstart.scenario.WelcomeEmailScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后演示发送一封注册欢迎邮件（Mock 传输）。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.email.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmailDemoRunner.class);

    private final WelcomeEmailScenarioService scenarioService;

    /**
     * @param scenarioService 欢迎邮件场景
     */
    public EmailDemoRunner(WelcomeEmailScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(ApplicationArguments args) {
        SendResult result = scenarioService.sendWelcome("u-1001", "Peach");
        log.info("email quickstart finished, success={}, provider={}, messageIdPresent={}",
                result.isSuccess(), result.getProvider(), result.getMessageId() != null);
    }
}
