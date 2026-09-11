package com.peach.email.quickstart;

import com.peach.email.core.SendResult;
import com.peach.email.quickstart.config.MockEmailTransportConfiguration;
import com.peach.email.quickstart.scenario.WelcomeEmailScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证注册欢迎邮件经 Mock 传输发送成功，并具备幂等跳过行为。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@SpringBootTest(properties = "quickstart.email.demo.enabled=false")
class WelcomeEmailScenarioTest {

    @Autowired
    private WelcomeEmailScenarioService scenarioService;

    @Autowired
    private MockEmailTransportConfiguration mockEmailTransportConfiguration;

    @BeforeEach
    void resetMock() {
        mockEmailTransportConfiguration.getMockEmailTransport().reset();
    }

    @Test
    void shouldSendWelcomeEmailViaMockTransport() {
        SendResult first = scenarioService.sendWelcome("it-user", "Tester");
        assertThat(first.isSuccess()).isTrue();
        assertThat(first.getProvider()).isEqualTo(MockEmailTransportConfiguration.PROVIDER_NAME);
        assertThat(mockEmailTransportConfiguration.getMockEmailTransport().getSendCount()).isEqualTo(1);
        assertThat(mockEmailTransportConfiguration.getMockEmailTransport().getSubjects())
                .containsExactly("Welcome, Tester");

        SendResult second = scenarioService.sendWelcome("it-user", "Tester");
        assertThat(second.isSuccess()).isTrue();
        assertThat(mockEmailTransportConfiguration.getMockEmailTransport().getSendCount()).isEqualTo(1);
    }
}
