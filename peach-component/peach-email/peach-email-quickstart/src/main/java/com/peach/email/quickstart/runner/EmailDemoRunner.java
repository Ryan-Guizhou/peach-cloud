package com.peach.email.quickstart.runner;

import com.peach.email.core.SendResult;
import com.peach.email.quickstart.config.MockEmailTransportConfiguration;
import com.peach.email.quickstart.example.AttachmentEmailExample;
import com.peach.email.quickstart.example.IdempotentSendExample;
import com.peach.email.quickstart.example.WelcomeEmailExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.UUID;

/**
 * 启动后演示 EmailSendService 的欢迎邮件、幂等发送与附件三种用法。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.email.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailDemoRunner implements ApplicationRunner {

    private final WelcomeEmailExample welcomeEmailExample;
    private final IdempotentSendExample idempotentSendExample;
    private final AttachmentEmailExample attachmentEmailExample;
    private final MockEmailTransportConfiguration mockConfig;

    @Override
    public void run(ApplicationArguments args) {
        runWelcomeDemo();
        runIdempotentDemo();
        runAttachmentDemo();
        log.info("email demo finished");
    }

    private void runWelcomeDemo() {
        mockConfig.getMockEmailTransport().reset();
        log.info("=== Welcome text/HTML send demo ===");
        SendResult result = welcomeEmailExample.sendWelcome("Peach");
        MockEmailTransportConfiguration.MockEmailTransport transport = mockConfig.getMockEmailTransport();
        log.info("welcome success={}, provider={}, sendCount={}, hadHtml={}",
                result.isSuccess(), result.getProvider(), transport.getSendCount(), transport.isLastHadHtml());
    }

    private void runIdempotentDemo() {
        log.info("=== sendAuto idempotency demo ===");
        int sendCount = idempotentSendExample.sendTwice("qs:email:idem:" + UUID.randomUUID());
        log.info("idempotent transportSends={}", sendCount);
    }

    private void runAttachmentDemo() {
        mockConfig.getMockEmailTransport().reset();
        log.info("=== Attachment send demo ===");
        SendResult result = attachmentEmailExample.sendGuide();
        MockEmailTransportConfiguration.MockEmailTransport transport = mockConfig.getMockEmailTransport();
        log.info("attachment success={}, sendCount={}, attachmentCount={}",
                result.isSuccess(), transport.getSendCount(), transport.getLastAttachmentCount());
    }
}
