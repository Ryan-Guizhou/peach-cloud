package com.peach.scheduler.rocket.jdbc;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.autoconfigure.PeachRocketAutoConfigure;
import com.peach.rocket.autoconfigure.PeachRocketOutboxAutoConfigure;
import com.peach.rocket.idempotent.InMemoryMqIdempotentStore;
import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.outbox.InMemoryMqOutboxStore;
import com.peach.rocket.outbox.MqOutboxStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;

/**
 * 调度 RocketMQ 可靠性校验自动配置。
 *
 * <p>启用 JDBC 可靠投递要求时，启动阶段校验 outbox 与幂等存储不能退回内存实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@AutoConfiguration
@AutoConfigureAfter({SchedulerRocketJdbcAutoConfiguration.class, PeachRocketAutoConfigure.class,
        PeachRocketOutboxAutoConfigure.class})
@ConditionalOnExpression("${peach.scheduler.enabled:true} && ${peach.scheduler.rocket.require-jdbc:true}")
@Indexed
public class SchedulerRocketDurabilityAutoConfiguration {

    /**
     * 创建 RocketMQ 可靠性存储校验器。
     *
     * @param outboxStoreProvider outbox 存储提供器。
     * @param idempotentStoreProvider 幂等存储提供器。
     * @return Spring 单例初始化完成后的可靠性校验器。
     */
    @Bean
    public SmartInitializingSingleton schedulerRocketDurabilityVerifier(
            ObjectProvider<MqOutboxStore> outboxStoreProvider,
            ObjectProvider<MqIdempotentStore> idempotentStoreProvider) {
        return () -> {
            MqOutboxStore outboxStore = outboxStoreProvider.getIfAvailable();
            MqIdempotentStore idempotentStore = idempotentStoreProvider.getIfAvailable();
            if (outboxStore == null) {
                throw new IllegalStateException("Scheduler RocketMQ requires a durable MqOutboxStore bean");
            }
            if (idempotentStore == null) {
                throw new IllegalStateException("Scheduler RocketMQ requires a durable MqIdempotentStore bean");
            }
            if (outboxStore instanceof InMemoryMqOutboxStore) {
                throw new IllegalStateException(
                        "Scheduler RocketMQ cannot use InMemoryMqOutboxStore when durable delivery is required");
            }
            if (idempotentStore instanceof InMemoryMqIdempotentStore) {
                throw new IllegalStateException(
                        "Scheduler RocketMQ cannot use InMemoryMqIdempotentStore when durable delivery is required");
            }
        };
    }
}
