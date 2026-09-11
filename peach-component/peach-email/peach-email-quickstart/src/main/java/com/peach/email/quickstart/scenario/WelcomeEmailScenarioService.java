package com.peach.email.quickstart.scenario;

import com.peach.email.core.EmailMessage;
import com.peach.email.core.SendResult;
import com.peach.email.service.EmailSendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 注册欢迎邮件场景：组装 EmailMessage 并通过 EmailSendService 发送。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class WelcomeEmailScenarioService {

    private final EmailSendService emailSendService;
    private final String from;
    private final String to;

    /**
     * @param emailSendService 邮件发送服务
     * @param from             发件人
     * @param to               收件人
     */
    public WelcomeEmailScenarioService(EmailSendService emailSendService,
                                       @Value("${quickstart.email.demo.from}") String from,
                                       @Value("${quickstart.email.demo.to}") String to) {
        this.emailSendService = emailSendService;
        this.from = from;
        this.to = to;
    }

    /**
     * 发送注册欢迎邮件。
     *
     * @param userId   业务用户标识，用于幂等键
     * @param userName 展示名称（仅进入主题，不作为敏感字段）
     * @return 发送结果
     */
    public SendResult sendWelcome(String userId, String userName) {
        String subject = "Welcome, " + userName;
        EmailMessage message = EmailMessage.builder()
                .from(from)
                .to(List.of(to))
                .subject(subject)
                .text("Thanks for registering. Your account is ready.")
                .idempotencyKey("welcome:" + userId)
                .build();
        return emailSendService.sendAuto(message);
    }
}
