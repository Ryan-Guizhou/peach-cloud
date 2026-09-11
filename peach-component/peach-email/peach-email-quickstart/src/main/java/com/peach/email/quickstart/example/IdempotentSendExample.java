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

/**
 * {@link EmailSendService#sendAuto} 幂等：相同业务键第二次调用不再走 Transport。
 *
 * <p>默认 {@code IdempotencyStore} 是进程内内存实现，不能作为多实例持久化保证。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@Slf4j
@Indexed
@Service
public class IdempotentSendExample {

    private final EmailSendService emailSendService;
    private final MockEmailTransportConfiguration mockConfig;
    private final String from;
    private final String to;

    public IdempotentSendExample(EmailSendService emailSendService,
                                 MockEmailTransportConfiguration mockConfig,
                                 @Value("${quickstart.email.demo.from}") String from,
                                 @Value("${quickstart.email.demo.to}") String to) {
        this.emailSendService = emailSendService;
        this.mockConfig = mockConfig;
        this.from = from;
        this.to = to;
    }

    /**
     * 使用同一幂等键连续调用两次 {@code sendAuto}。
     *
     * @param idempotencyKey 稳定业务幂等键
     * @return Mock Transport 实际外发次数
     */
    public int sendTwice(String idempotencyKey) {
        mockConfig.getMockEmailTransport().reset();
        EmailMessage message = EmailMessage.builder()
                .from(from)
                .to(List.of(to))
                .subject("Idempotent notice")
                .text("Idempotent body.")
                .idempotencyKey(idempotencyKey)
                .build();
        SendResult first = emailSendService.sendAuto(message);
        SendResult second = emailSendService.sendAuto(message);
        int sendCount = mockConfig.getMockEmailTransport().getSendCount();
        log.info("idempotent send, firstSuccess={}, secondSuccess={}, transportSends={}",
                first.isSuccess(), second.isSuccess(), sendCount);
        return sendCount;
    }
}
