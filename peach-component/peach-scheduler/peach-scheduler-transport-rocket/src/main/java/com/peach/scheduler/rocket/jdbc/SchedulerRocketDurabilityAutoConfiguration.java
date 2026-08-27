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
 * 验证相关行为。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
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
     * 创建相关对象。
     */
    public SchedulerRocketDurabilityAutoConfiguration() {
        // Intentionally empty.
    }

    /**
     * 创建相关对象。
     *
     * @param outboxStoreProvider 参数说明
     * @param idempotentStoreProvider 参数说明
     * @return 返回结果
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
