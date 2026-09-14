package com.peach.openfeign.quickstart.runner;

import com.peach.openfeign.exception.PeachFeignRemoteException;
import com.peach.openfeign.exception.PeachFeignTimeoutException;
import com.peach.openfeign.quickstart.example.LocalStubCallExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.Map;

/**
 * 启动后演示本机 Stub 的成功调用，以及 ErrorDecoder 对 500/408 的分类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.openfeign.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenFeignDemoRunner implements ApplicationRunner {

    private final LocalStubCallExample localStubCallExample;

    @Override
    public void run(ApplicationArguments args) {
        runSuccessDemo();
        runErrorDemo();
        runTimeoutDemo();
        log.info("openfeign demo finished");
    }

    private void runSuccessDemo() {
        log.info("=== Feign success call demo ===");
        Map<String, String> body = localStubCallExample.echo("hello-feign");
        if (!"stub".equals(body.get("source")) || !"hello-feign".equals(body.get("message"))) {
            throw new IllegalStateException("feign echo self-check failed");
        }
    }

    private void runErrorDemo() {
        log.info("=== Feign ErrorDecoder remote error demo ===");
        PeachFeignRemoteException exception = localStubCallExample.failAsRemoteError();
        if (exception.getStatus() != 500) {
            throw new IllegalStateException("expected remote status 500");
        }
    }

    private void runTimeoutDemo() {
        log.info("=== Feign ErrorDecoder timeout demo ===");
        PeachFeignTimeoutException exception = localStubCallExample.timeoutAsClassified();
        if (exception.getClientName() == null || exception.getClientName().isBlank()) {
            throw new IllegalStateException("expected classified timeout exception");
        }
    }
}
