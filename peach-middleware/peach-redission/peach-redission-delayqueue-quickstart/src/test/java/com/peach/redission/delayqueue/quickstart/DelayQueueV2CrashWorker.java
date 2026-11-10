package com.peach.redission.delayqueue.quickstart;

import com.peach.redission.delayqueue.v2.DelayQueueV2Properties;
import com.peach.redission.delayqueue.v2.RedisDelayQueueV2;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 测试专用的独立 JVM Worker：认领消息、写入就绪标记后等待父进程强制终止。
 *
 * <p>不在应用生产组件内注册，也不执行消息 ACK 或优雅退出；
 * 仅允许独立 GitHub Actions Redis 7.2 服务容器使用。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
public final class DelayQueueV2CrashWorker {

    private DelayQueueV2CrashWorker() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !"true".equals(System.getenv("RUN_REDIS_V2_IT"))) {
            throw new IllegalArgumentException("Crash worker is restricted to opt-in Redis CI");
        }
        String topic = args[0];
        Path marker = Path.of(args[1]);

        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient client = Redisson.create(config);
        DelayQueueV2Properties properties = new DelayQueueV2Properties();
        properties.setEnabled(true);
        properties.setLease(Duration.ofSeconds(3));
        properties.setMaxAttempts(2);
        properties.setRetryInitial(Duration.ofMillis(1));
        properties.setRetryMax(Duration.ofMillis(10));

        try (RedisDelayQueueV2 queue = new RedisDelayQueueV2(client, properties)) {
            var messages = queue.claim(topic);
            if (messages.size() != 1) {
                throw new IllegalStateException("Expected one claim in independent crash worker");
            }
            RedisDelayQueueV2.ClaimedMessage item = messages.get(0);
            Files.writeString(marker, item.id() + "|" + item.fencingToken() + "|" + item.payload(),
                    StandardCharsets.UTF_8);
            Thread.sleep(Duration.ofMinutes(2));
            throw new IllegalStateException("Crash worker was not killed by test parent");
        } finally {
            // In the intended test path, SIGKILL prevents any graceful cleanup.
            client.shutdown();
        }
    }
}
