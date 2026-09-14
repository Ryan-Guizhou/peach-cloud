package com.peach.email.quickstart.example;

import com.peach.email.core.Attachment;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.SendResult;
import com.peach.email.quickstart.config.MockEmailTransportConfiguration;
import com.peach.email.service.EmailSendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * 通过 {@link EmailMessage} 携带带字节内容的附件，并由 Mock Transport 记录附件数量。
 *
 * <p>三参数 {@code Attachment} 不携带数据源，当前 MIME 构建会跳过该路径；本样例使用五参数构造。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@Slf4j
@Indexed
@Service
public class AttachmentEmailExample {

    private static final byte[] GUIDE_BYTES = "peach-email quickstart guide".getBytes(StandardCharsets.UTF_8);

    private final EmailSendService emailSendService;
    private final String from;
    private final String to;

    public AttachmentEmailExample(EmailSendService emailSendService,
                                  @Value("${quickstart.email.demo.from}") String from,
                                  @Value("${quickstart.email.demo.to}") String to) {
        this.emailSendService = emailSendService;
        this.from = from;
        this.to = to;
    }

    /**
     * 指定 mock Provider 发送带一份文本附件的邮件。
     *
     * @return 发送结果
     */
    public SendResult sendGuide() {
        Attachment attachment = new Attachment(
                "guide.txt",
                "text/plain",
                GUIDE_BYTES,
                null,
                Attachment.AttachmentType.ATTACHMENT.getType());
        EmailMessage message = EmailMessage.builder()
                .from(from)
                .to(List.of(to))
                .subject("Quickstart guide")
                .text("See the attached guide.")
                .addAttachment(attachment)
                .idempotencyKey("qs:email:attach:" + UUID.randomUUID())
                .build();
        SendResult result = emailSendService.send(MockEmailTransportConfiguration.PROVIDER_NAME, message);
        log.info("attachment send, success={}, provider={}", result.isSuccess(), result.getProvider());
        return result;
    }
}
