package com.peach.redis.quickstart;

import com.peach.redis.autoconfigure.PeachRedisAutoConfiguration;
import com.peach.redis.autoconfigure.PeachRedisProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在测试容器中停止真实 Sentinel master，验证既有 Jedis 和 Redisson 实例原位恢复。
 *
 * <p>只在 CI 专用 `RUN_REDIS_SENTINEL_LIVE_IT` 标志为 true 时执行，严禁在
 * 生产 Redis/Nacos 上执行。停止操作只针对 CI 创建的 peach-it-master 容器。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_SENTINEL_LIVE_IT", matches = "true")
class SentinelLiveClientFailoverServerIntegrationTest {

    @Test
    void existingClientInstancesReconnectToPromotedMasterAfterActualContainerOutage() throws Exception {
        String sentinels = System.getenv("REDIS_TOPOLOGY_HOSTS");
        String master = System.getenv("REDIS_TOPOLOGY_SENTINEL_MASTER");
        assertThat(sentinels).as("Sentinel host list must come from isolated CI").isNotBlank();
        assertThat(master).as("Sentinel master name must come from isolated CI").isNotBlank();

        PeachRedisProperties properties = new PeachRedisProperties();
        properties.setMode("sentinel");
        properties.setHost(sentinels);
        properties.setSentinelMaster(master);
        properties.setDatabase(0);
        PeachRedisAutoConfiguration autoConfiguration = new PeachRedisAutoConfiguration();
        JedisConnectionFactory jedis = autoConfiguration.jedisConnectionFactory(
                autoConfiguration.redisPoolConfigs(properties), properties);
        jedis.afterPropertiesSet();

        Config redissonConfiguration = new Config();
        redissonConfiguration.setCodec(new JsonJacksonCodec());
        redissonConfiguration.useSentinelServers()
                .setMasterName(master)
                .setDatabase(0)
                .setReadMode(ReadMode.MASTER)
                .addSentinelAddress(Arrays.stream(sentinels.split(","))
                        .map(String::trim).map(host -> "redis://" + host).toArray(String[]::new));
        RedissonClient redisson = null;
        StringRedisTemplate template = new StringRedisTemplate(jedis);
        template.afterPropertiesSet();

        String prefix = "sentinel:live-client:" + UUID.randomUUID();
        String jedisKey = prefix + ":jedis";
        String redissonKey = prefix + ":redisson";
        try {
            redisson = Redisson.create(redissonConfiguration);
            template.opsForValue().set(jedisKey, "before", Duration.ofSeconds(120));
            redisson.getBucket(redissonKey).set("before", 120, TimeUnit.SECONDS);
            assertThat(template.opsForValue().get(jedisKey)).isEqualTo("before");
            assertThat(redisson.getBucket(redissonKey).get()).isEqualTo("before");

            Process process = new ProcessBuilder("docker", "stop", "--time", "1", "peach-it-master")
                    .redirectErrorStream(true).start();
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Timed out stopping test-only Redis master container");
            }
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Failed to stop test-only Redis master container");
            }

            // No client or factory is re-created here. Failover takes several seconds.
            boolean recovered = false;
            RuntimeException lastFailure = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(100);
            while (System.nanoTime() < deadline) {
                try {
                    template.opsForValue().set(jedisKey, "after", Duration.ofSeconds(40));
                    redisson.getBucket(redissonKey).set("after", 40, TimeUnit.SECONDS);
                    String jedisValue = template.opsForValue().get(jedisKey);
                    Object redissonValue = redisson.getBucket(redissonKey).get();
                    if ("after".equals(jedisValue) && "after".equals(redissonValue)) {
                        recovered = true;
                        break;
                    }
                } catch (RuntimeException exception) {
                    lastFailure = exception;
                }
                Thread.sleep(1000);
            }
            assertThat(recovered).as("Jedis and Redisson must recover using their original instances: "
                    + (lastFailure == null ? "no successful round-trip" : lastFailure.getMessage())).isTrue();
            assertThat(template.getExpire(jedisKey)).isGreaterThan(0L);
            assertThat(redisson.getBucket(redissonKey).remainTimeToLive()).isGreaterThan(0L);
        } finally {
            try {
                template.delete(jedisKey);
                if (redisson != null) {
                    redisson.getBucket(redissonKey).delete();
                }
            } catch (RuntimeException ignored) {
                // The test must still release both clients if Redis remains unavailable.
            } finally {
                if (redisson != null) {
                    redisson.shutdown();
                }
                jedis.destroy();
            }
        }
    }
}
