package com.peach.redission.bloomfilter.quickstart;

import com.peach.redis.bloom.config.BloomFilterProperties;
import com.peach.redis.bloom.core.BloomStatus;
import com.peach.redis.bloom.core.SegmentedBloomFilterService;
import com.peach.redis.bloom.spi.BloomScalePolicy;
import com.peach.redis.bloom.spi.CodecProvider;
import com.peach.redis.bloom.spi.KeyNamingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 两个物理独立 RedissonClient 验证跨节点布隆分段扩容、元数据可见性。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
class BloomTwoClientServerIntegrationTest {

    @Test
    void secondClientSeesNewSegmentsAndIndependentCounters() {
        RedissonClient firstClient = client();
        RedissonClient secondClient = client();
        String namespace = "bloom-" + UUID.randomUUID();
        BloomFilterProperties properties = new BloomFilterProperties();
        properties.setKeyPrefix("twoclient:bloom");
        properties.setInitialCapacity(2L);
        properties.setLoadFactor(0.9d);
        properties.setMaxSegments(8);
        properties.setEnableLocalCache(true);
        SegmentedBloomFilterService first = new SegmentedBloomFilterService(firstClient, properties,
                CodecProvider.defaultJacksonCodec(), KeyNamingStrategy.defaultStrategy(),
                BloomScalePolicy.defaultPolicy());
        SegmentedBloomFilterService second = new SegmentedBloomFilterService(secondClient, properties,
                CodecProvider.defaultJacksonCodec(), KeyNamingStrategy.defaultStrategy(),
                BloomScalePolicy.defaultPolicy());

        try {
            first.clear(namespace);
            first.initNamespace(namespace, 2L, 0.001d);
            assertThat(second.mightContain(namespace, "never-added")).isFalse();
            assertThat(first.add(namespace, "sku-1")).isTrue();
            assertThat(first.add(namespace, "sku-2")).isTrue();
            assertThat(first.segments(namespace)).isGreaterThanOrEqualTo(2);
            assertThat(first.add(namespace, "sku-3")).isTrue();

            assertThat(second.mightContain(namespace, "sku-3")).isTrue();
            assertThat(second.mightContainAll(namespace,
                    List.of("sku-1", "sku-2", "sku-3"))).isTrue();
            BloomStatus state = second.status(namespace);
            assertThat(state.segmentStatuses).hasSizeGreaterThanOrEqualTo(2);
            assertThat(state.segmentStatuses.get(0).countApprox).isGreaterThanOrEqualTo(2L);
            assertThat(state.segmentStatuses.get(1).countApprox).isGreaterThanOrEqualTo(1L);
        } finally {
            try {
                first.clear(namespace);
            } finally {
                secondClient.shutdown();
                firstClient.shutdown();
            }
        }
    }

    private static RedissonClient client() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(0);
        return Redisson.create(config);
    }
}
