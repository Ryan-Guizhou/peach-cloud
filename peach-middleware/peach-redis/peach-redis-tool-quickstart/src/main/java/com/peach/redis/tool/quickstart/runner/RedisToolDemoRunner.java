package com.peach.redis.tool.quickstart.runner;

import com.peach.redis.tool.quickstart.example.SessionStringExample;
import com.peach.redis.tool.quickstart.example.RedisToolOperationsExample;
import com.peach.redis.tool.quickstart.config.RedisToolDemoContext;
import com.peach.redis.tool.quickstart.config.RedisToolDemoProperties;
import com.peach.redis.tool.quickstart.enums.RedisToolKey;
import com.peach.redis.tool.quickstart.example.UserProfileHashExample;
import com.peach.redis.tool.quickstart.example.UserTagSetExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * 启动后演示全部 RedisDao 操作；前置清理必执行，后置清理由配置控制。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.redis-tool.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisToolDemoRunner implements ApplicationRunner {

    private final SessionStringExample sessionStringExample;
    private final UserProfileHashExample userProfileHashExample;
    private final UserTagSetExample userTagSetExample;

    private final RedisToolOperationsExample operations;
    private final RedisToolDemoContext context;
    private final RedisToolDemoProperties properties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        operations.prepare();
        context.key(RedisToolKey.SESSION, "s-1001");
        context.key(RedisToolKey.PROFILE, "u-2001");
        context.key(RedisToolKey.TAGS, "u-2001");
        context.clear();
        Exception failure = null;
        try {
            runSessionDemo();
            runProfileDemo();
            runTagDemo();
            operations.run();
            log.info("Redis tool demo finished, runId={}, cleanupAfterRun={}",
                    properties.getRunId(), properties.isCleanupAfterRun());
        } catch (Exception exception) {
            failure = exception;
            throw exception;
        } finally {
            if (properties.isCleanupAfterRun()) {
                try {
                    context.clear();
                } catch (RuntimeException cleanupFailure) {
                    if (failure == null) {
                        throw cleanupFailure;
                    }
                    failure.addSuppressed(cleanupFailure);
                }
            }
        }
    }

    private void runSessionDemo() {
        log.info("=== Session String + TTL demo ===");
        sessionStringExample.save("s-1001", "token-abc", Duration.ofMinutes(30));
        String token = sessionStringExample.get("s-1001");
        log.info("session exists={}, tokenPresent={}, tokenLength={}",
                sessionStringExample.exists("s-1001"), token != null, token == null ? 0 : token.length());
    }

    private void runProfileDemo() {
        log.info("=== User Profile Hash demo ===");
        userProfileHashExample.saveProfile("u-2001", "Peach", "Shanghai");
        Map<Object, Object> all = userProfileHashExample.getAll("u-2001");
        log.info("Profile field count={}", all.size());
        userProfileHashExample.removeCity("u-2001");
        log.info("Profile field count after removal={}", userProfileHashExample.getAll("u-2001").size());
    }

    private void runTagDemo() {
        log.info("=== User Tag Set demo ===");
        userTagSetExample.addTags("u-2001", "vip", "beta", "cn");
        Set<Object> tags = userTagSetExample.listTags("u-2001");
        log.info("Tag count={}", tags.size());
        userTagSetExample.removeTag("u-2001", "beta");
        log.info("Tag count after removal={}", userTagSetExample.listTags("u-2001").size());
    }
}
