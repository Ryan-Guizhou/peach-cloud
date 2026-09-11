package com.peach.initialize.quickstart;

import com.peach.initialize.constant.InitializeHandlerType;
import com.peach.initialize.quickstart.example.HandlerOrderExample;
import com.peach.initialize.quickstart.example.WarmupStatusExample;
import com.peach.initialize.quickstart.handler.CacheWarmupInitializeHandler;
import com.peach.initialize.quickstart.status.InitializeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证自定义 {@code InitializeHandler} 在启动编排中写入成功标记，并按 {@code executeOrder} 执行。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:20
 */
@SpringBootTest(properties = "quickstart.initialize.demo.enabled=false")
class InitializeCapabilityTest {

    @Autowired
    private WarmupStatusExample warmupStatusExample;

    @Autowired
    private HandlerOrderExample handlerOrderExample;

    @Autowired
    private CacheWarmupInitializeHandler cacheWarmupInitializeHandler;

    @Test
    void shouldMarkWarmupSuccessAfterStartedEvent() {
        assertThat(cacheWarmupInitializeHandler.type()).isEqualTo(InitializeHandlerType.APP_EVENT_LISTENER);
        assertThat(warmupStatusExample.isWarmedUp()).isTrue();
        assertThat(warmupStatusExample.warmupPayload()).isEqualTo(InitializeStatus.WARMUP_PAYLOAD);
    }

    @Test
    void shouldExecuteSameTypeHandlersByExecuteOrder() {
        assertThat(handlerOrderExample.warmupOrder()).isLessThan(handlerOrderExample.resourceCheckOrder());
        assertThat(handlerOrderExample.executedHandlers()).isEqualTo(List.of(
                InitializeStatus.HANDLER_CACHE_WARMUP,
                InitializeStatus.HANDLER_RESOURCE_CHECK));
    }
}
