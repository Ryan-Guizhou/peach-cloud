package com.peach.rocket.quickstart.config;

import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.idempotent.InMemoryMqIdempotentStore;
import com.peach.rocket.idempotent.MqIdempotentStore;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

/**
 * Quickstart 进程内装配：提供可投递的 {@link MqPublisher} 与 starter 默认内存幂等存储。
 *
 * <p>关闭 {@code peach.rocket.enabled} 后 starter 不再创建依赖 {@code RocketMQTemplate} 的 Bean，
 * 由本配置补齐演示与测试所需契约实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Indexed
@Configuration
public class InProcessRocketConfiguration {

    /**
     * 进程内发布器，按 {@code @MqEvent}/{@code @MqConsumer} 的 topic+tag 同步投递给 Handler。
     */
    @Bean
    public InProcessMqPublisher mqPublisher(ApplicationContext applicationContext) {
        return new InProcessMqPublisher(applicationContext);
    }

    /**
     * 与 starter 默认实现相同的内存幂等存储，仅用于单进程演示。
     */
    @Bean
    public MqIdempotentStore mqIdempotentStore() {
        return new InMemoryMqIdempotentStore();
    }
}
