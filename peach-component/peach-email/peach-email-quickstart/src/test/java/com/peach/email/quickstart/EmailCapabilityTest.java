package com.peach.email.quickstart;

import com.peach.email.core.SendResult;
import com.peach.email.quickstart.config.MockEmailTransportConfiguration;
import com.peach.email.quickstart.example.AttachmentEmailExample;
import com.peach.email.quickstart.example.IdempotentSendExample;
import com.peach.email.quickstart.example.WelcomeEmailExample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 EmailSendService 欢迎邮件、幂等发送与附件能力（Mock Transport，不外发）。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@SpringBootTest(properties = "quickstart.email.demo.enabled=false")
class EmailCapabilityTest {

    @Autowired
    private WelcomeEmailExample welcomeEmailExample;

    @Autowired
    private IdempotentSendExample idempotentSendExample;

    @Autowired
    private AttachmentEmailExample attachmentEmailExample;

    @Autowired
    private MockEmailTransportConfiguration mockConfig;

    @BeforeEach
    void resetMock() {
        mockConfig.getMockEmailTransport().reset();
    }

    @Test
    void welcomeShouldSendTextAndHtmlOnce() {
        SendResult result = welcomeEmailExample.sendWelcome("Peach");
        MockEmailTransportConfiguration.MockEmailTransport transport = mockConfig.getMockEmailTransport();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getProvider()).isEqualTo(MockEmailTransportConfiguration.PROVIDER_NAME);
        assertThat(transport.getSendCount()).isEqualTo(1);
        assertThat(transport.isLastHadHtml()).isTrue();
    }

    @Test
    void sendAutoShouldSkipDuplicateIdempotencyKey() {
        int sendCount = idempotentSendExample.sendTwice("qs:email:test:idem:" + UUID.randomUUID());
        assertThat(sendCount).isEqualTo(1);
    }

    @Test
    void attachmentShouldReachMockTransport() {
        SendResult result = attachmentEmailExample.sendGuide();
        MockEmailTransportConfiguration.MockEmailTransport transport = mockConfig.getMockEmailTransport();

        assertThat(result.isSuccess()).isTrue();
        assertThat(transport.getSendCount()).isEqualTo(1);
        assertThat(transport.getLastAttachmentCount()).isEqualTo(1);
    }
}
