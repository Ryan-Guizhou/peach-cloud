package com.peach.email.quickstart.config;

import com.peach.email.core.EmailContext;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.EmailTransport;
import com.peach.email.core.SendResult;
import com.peach.email.router.ProviderRouter;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注册内存 Mock 传输，避免 QuickStart 依赖真实 SMTP。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
public class MockEmailTransportConfiguration {

    public static final String PROVIDER_NAME = "mock";

    private final ProviderRouter providerRouter;
    private final MockEmailTransport mockEmailTransport = new MockEmailTransport();

    /**
     * @param providerRouter 提供商路由器
     */
    public MockEmailTransportConfiguration(ProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }

    @PostConstruct
    public void registerMockProvider() {
        EmailContext context = new EmailContext("localhost", 25, false,
                "demo@example.com", "unused-placeholder", new Properties());
        providerRouter.register(PROVIDER_NAME, mockEmailTransport, context);
        providerRouter.setPriority(PROVIDER_NAME, 0);
    }

    /**
     * @return Mock 传输，供测试断言发送次数
     */
    public MockEmailTransport getMockEmailTransport() {
        return mockEmailTransport;
    }

    /**
     * 内存邮件传输：只记录发送次数，不落真实 SMTP。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/9/11 16:40
     */
    public static final class MockEmailTransport implements EmailTransport {

        private final AtomicInteger sendCount = new AtomicInteger();
        private final List<String> subjects = Collections.synchronizedList(new ArrayList<>());

        @Override
        public String getName() {
            return PROVIDER_NAME;
        }

        @Override
        public SendResult send(EmailMessage emailMessage, EmailContext emailContext) {
            sendCount.incrementAndGet();
            if (emailMessage != null && emailMessage.getSubject() != null) {
                subjects.add(emailMessage.getSubject());
            }
            return new SendResult(PROVIDER_NAME, UUID.randomUUID().toString(), 1L, true, null);
        }

        public int getSendCount() {
            return sendCount.get();
        }

        public List<String> getSubjects() {
            return List.copyOf(subjects);
        }

        public void reset() {
            sendCount.set(0);
            subjects.clear();
        }
    }
}
