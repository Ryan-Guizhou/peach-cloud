package com.peach.redis.tool.quickstart;

import com.peach.redis.tool.quickstart.example.SessionStringExample;
import com.peach.redis.tool.quickstart.example.UserProfileHashExample;
import com.peach.redis.tool.quickstart.example.UserTagSetExample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 RedisDao 对外能力：String+TTL、Hash、Set。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@SpringBootTest(properties = "quickstart.redis-tool.demo.enabled=false")
@Testcontainers(disabledWithoutDocker = true)
class RedisToolCapabilityTest {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("peach.redis.mode", () -> "standalone");
        registry.add("peach.redis.host", () -> REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
        registry.add("peach.redis.password", () -> "");
        registry.add("peach.redis.database", () -> "0");
    }

    @Autowired
    private SessionStringExample sessionStringExample;

    @Autowired
    private UserProfileHashExample userProfileHashExample;

    @Autowired
    private UserTagSetExample userTagSetExample;

    @BeforeEach
    void clean() {
        sessionStringExample.delete("s-1001");
        userProfileHashExample.deleteProfile("u-2001");
        userTagSetExample.clear("u-2001");
    }

    @Test
    void sessionStringShouldSaveGetAndDelete() {
        sessionStringExample.save("s-1001", "token-abc", Duration.ofMinutes(10));
        assertThat(sessionStringExample.exists("s-1001")).isTrue();
        assertThat(sessionStringExample.get("s-1001")).isEqualTo("token-abc");

        sessionStringExample.delete("s-1001");
        assertThat(sessionStringExample.exists("s-1001")).isFalse();
        assertThat(sessionStringExample.get("s-1001")).isNull();
    }

    @Test
    void profileHashShouldWriteReadAndFieldDelete() {
        userProfileHashExample.saveProfile("u-2001", "Peach", "Shanghai");
        assertThat(userProfileHashExample.getNickname("u-2001")).isEqualTo("Peach");

        Map<Object, Object> all = userProfileHashExample.getAll("u-2001");
        assertThat(all).containsEntry("nickname", "Peach").containsEntry("city", "Shanghai");

        userProfileHashExample.removeCity("u-2001");
        assertThat(userProfileHashExample.getAll("u-2001")).containsKey("nickname").doesNotContainKey("city");
    }

    @Test
    void tagSetShouldAddListAndRemove() {
        userTagSetExample.addTags("u-2001", "vip", "beta", "cn");
        Set<Object> tags = userTagSetExample.listTags("u-2001");
        assertThat(tags).contains("vip", "beta", "cn");

        userTagSetExample.removeTag("u-2001", "beta");
        assertThat(userTagSetExample.listTags("u-2001")).contains("vip", "cn").doesNotContain("beta");
    }
}
