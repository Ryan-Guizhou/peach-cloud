package com.peach.email.quickstart.example;

import com.peach.email.core.EmailMessage;
import com.peach.email.core.SendResult;
import com.peach.email.quickstart.config.MockEmailTransportConfiguration;
import com.peach.email.service.EmailSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 指定 Provider 发送欢迎邮件：同时携带纯文本降级正文与 HTML。
 *
 * <p>模板渲染不在 {@link EmailSendService#sendAuto} 内自动发生，且内置
 * {@code TemplateManager#resolve} 对有效路径判断有误，本样例直接设置 HTML。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@Slf4j
@Indexed
@Service
public class WelcomeEmailExample {

    private final EmailSendService emailSendService;
    private final String from;
    private final String to;

    public WelcomeEmailExample(EmailSendService emailSendService,
                               @Value("${quickstart.email.demo.from}") String from,
                               @Value("${quickstart.email.demo.to}") String to) {
        this.emailSendService = emailSendService;
        this.from = from;
        this.to = to;
    }

    /**
     * 通过 {@link EmailSendService#send(String, EmailMessage)} 指定 mock Provider 发送。
     *
     * @param displayName 展示名，仅用于固定演示文案
     * @return 发送结果
     */
    public SendResult sendWelcome(String displayName) {
        String name = displayName == null || displayName.isBlank() ? "Peach" : displayName;
        EmailMessage message = EmailMessage.builder()
                .from(from)
                .to(List.of(to))
                .subject("Welcome to Peach")
                .text("Welcome, " + name + ".")
                .html("<p>Welcome, <b>" + name + "</b>.</p>")
                .idempotencyKey("qs:email:welcome:" + UUID.randomUUID())
                .build();
        SendResult result = emailSendService.send(MockEmailTransportConfiguration.PROVIDER_NAME, message);
        log.info("welcome send, success={}, provider={}", result.isSuccess(), result.getProvider());
        return result;
    }
}
