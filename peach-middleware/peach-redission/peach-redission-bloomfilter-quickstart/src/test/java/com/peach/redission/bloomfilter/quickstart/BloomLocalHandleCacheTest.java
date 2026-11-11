package com.peach.redission.bloomfilter.quickstart;

import com.peach.redis.bloom.config.BloomFilterProperties;
import com.peach.redis.bloom.core.SegmentedBloomFilterService;
import com.peach.redis.bloom.spi.BloomScalePolicy;
import com.peach.redis.bloom.spi.CodecProvider;
import com.peach.redis.bloom.spi.KeyNamingStrategy;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证动态命名空间下的 BloomFilter 本地句柄缓存上限。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class BloomLocalHandleCacheTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldEvictOldHandlesWhenLocalCacheIsFull() {
        BloomFilterProperties props = new BloomFilterProperties();
        props.setLocalCacheMaximumSize(2);
        RedissonClient client = mock(RedissonClient.class);
        RList<String> segments = mock(RList.class);
        RBloomFilter<Object> filter = mock(RBloomFilter.class);
        doReturn(segments).when(client).getList(anyString());
        when(segments.readAll()).thenReturn(List.of("segment-a"), List.of("segment-b"),
                List.of("segment-c"), List.of("segment-a"));
        when(client.getBloomFilter(anyString(), any(Codec.class))).thenReturn(filter);
        SegmentedBloomFilterService service = new SegmentedBloomFilterService(client, props,
                CodecProvider.defaultJacksonCodec(), KeyNamingStrategy.defaultStrategy(),
                BloomScalePolicy.defaultPolicy());

        service.mightContain("dynamic", "value");
        service.mightContain("dynamic", "value");
        service.mightContain("dynamic", "value");
        service.mightContain("dynamic", "value");

        verify(client, times(2)).getBloomFilter(eq("segment-a"), any(Codec.class));
    }

    @Test
    void shouldRejectNonPositiveCapacity() {
        BloomFilterProperties props = new BloomFilterProperties();
        assertThatThrownBy(() -> props.setLocalCacheMaximumSize(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
