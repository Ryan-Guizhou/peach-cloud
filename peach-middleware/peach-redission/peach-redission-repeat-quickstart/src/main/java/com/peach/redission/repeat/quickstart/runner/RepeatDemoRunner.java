package com.peach.redission.repeat.quickstart.runner;

import com.peach.redission.repeat.quickstart.example.OrderSubmitRepeatExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示同 key 二次拒绝与不同 key 互不影响。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:00
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.repeat.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RepeatDemoRunner implements ApplicationRunner {

    private final OrderSubmitRepeatExample orderSubmitRepeatExample;

    @Override
    public void run(ApplicationArguments args) {
        runSameKeyRejected();
        runDifferentKeysIndependent();
        log.info("repeat demo finished");
    }

    private void runSameKeyRejected() {
        log.info("=== Same key rejected on second submit ===");
        String requestId = "req-same-" + System.nanoTime();
        String first = orderSubmitRepeatExample.submitOrder(requestId);
        log.info("first submit result={}", first);
        boolean blocked = false;
        String message = null;
        try {
            orderSubmitRepeatExample.submitOrder(requestId);
        } catch (IllegalStateException ex) {
            blocked = true;
            message = ex.getMessage();
        }
        if (!blocked) {
            throw new IllegalStateException("same-key second submit should be rejected");
        }
        log.info("second submit blocked, message={}", message);
    }

    private void runDifferentKeysIndependent() {
        log.info("=== Different keys do not interfere ===");
        long nano = System.nanoTime();
        String firstId = "req-ind-a-" + nano;
        String secondId = "req-ind-b-" + nano;
        String first = orderSubmitRepeatExample.submitOrder(firstId);
        String second = orderSubmitRepeatExample.submitOrder(secondId);
        log.info("different keys first={}, second={}", first, second);
    }
}
