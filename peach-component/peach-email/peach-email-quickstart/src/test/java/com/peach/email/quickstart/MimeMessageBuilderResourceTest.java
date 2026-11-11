package com.peach.email.quickstart;

import com.peach.email.build.MimeMessageBuilder;
import com.peach.email.core.Attachment;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.InlineResource;
import jakarta.mail.Session;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证附件和内嵌资源无内容时不得静默发送不完整邮件。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 11:30
 */
class MimeMessageBuilderResourceTest {

    private final Session session = Session.getInstance(new Properties());

    @Test
    void shouldRejectMissingAttachmentPayload() {
        EmailMessage message = basicMessage()
                .attachments(List.of(new Attachment("report.pdf", "application/pdf", "attachment")))
                .build();

        assertThatThrownBy(() -> MimeMessageBuilder.build(session, message))
                .isInstanceOf(jakarta.mail.MessagingException.class)
                .hasMessageContaining("Attachment requires content");
    }

    @Test
    void shouldRejectMissingInlineResourceFile() {
        EmailMessage message = basicMessage()
                .html("<img src='cid:logo'>")
                .inlineResources(List.of(new InlineResource("logo", "image/png", null,
                        "/nonexistent/peach-mail-logo.png")))
                .build();

        assertThatThrownBy(() -> MimeMessageBuilder.build(session, message))
                .isInstanceOf(jakarta.mail.MessagingException.class)
                .hasMessageContaining("Inline resource file is not readable");
    }

    private static EmailMessage.Builder basicMessage() {
        return EmailMessage.builder()
                .from("sender@example.invalid")
                .to(List.of("receiver@example.invalid"))
                .subject("Test")
                .text("Test message");
    }
}
