package com.peach.redis.tool.quickstart.runner;

import com.peach.redis.tool.quickstart.example.SessionStringExample;
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
 * 启动后演示 RedisDao 的 String/Hash/Set 典型用法。
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

    @Override
    public void run(ApplicationArguments args) {
        runSessionDemo();
        runProfileDemo();
        runTagDemo();
        log.info("redis-tool demo finished");
    }

    private void runSessionDemo() {
        log.info("=== Session String + TTL demo ===");
        sessionStringExample.delete("s-1001");
        sessionStringExample.save("s-1001", "token-abc", Duration.ofMinutes(30));
        log.info("session exists={}, value={}", sessionStringExample.exists("s-1001"), sessionStringExample.get("s-1001"));
        sessionStringExample.delete("s-1001");
        log.info("session after delete exists={}", sessionStringExample.exists("s-1001"));
    }

    private void runProfileDemo() {
        log.info("=== User Profile Hash demo ===");
        userProfileHashExample.deleteProfile("u-2001");
        userProfileHashExample.saveProfile("u-2001", "Peach", "Shanghai");
        Map<Object, Object> all = userProfileHashExample.getAll("u-2001");
        log.info("profile all={}, nickname={}", all, userProfileHashExample.getNickname("u-2001"));
        userProfileHashExample.removeCity("u-2001");
        log.info("profile after removeCity={}", userProfileHashExample.getAll("u-2001"));
        userProfileHashExample.deleteProfile("u-2001");
    }

    private void runTagDemo() {
        log.info("=== User Tag Set demo ===");
        userTagSetExample.clear("u-2001");
        userTagSetExample.addTags("u-2001", "vip", "beta", "cn");
        Set<Object> tags = userTagSetExample.listTags("u-2001");
        log.info("tags={}", tags);
        userTagSetExample.removeTag("u-2001", "beta");
        log.info("tags after remove={}", userTagSetExample.listTags("u-2001"));
        userTagSetExample.clear("u-2001");
    }
}
