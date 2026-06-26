package com.peach.rocket.autoconfigure;

import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.outbox.DefaultMqOutboxPublisher;
import com.peach.rocket.outbox.DefaultMqOutboxReplayService;
import com.peach.rocket.outbox.InMemoryMqOutboxStore;
import com.peach.rocket.outbox.MqOutboxDispatcher;
import com.peach.rocket.outbox.MqOutboxPublisher;
import com.peach.rocket.outbox.MqOutboxReplayService;
import com.peach.rocket.outbox.MqOutboxStore;
import com.peach.rocket.route.MqRouteResolver;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Peach RocketMQ Outbox 可靠消息自动配置。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@EnableScheduling
@AutoConfiguration
@AutoConfigureAfter(PeachRocketAutoConfigure.class)
@EnableConfigurationProperties(PeachRocketProperties.class)
@ConditionalOnProperty(prefix = "peach.rocket.outbox", name = "enabled", havingValue = "true")
public class PeachRocketOutboxAutoConfigure {


    @Bean
    @ConditionalOnMissingBean
    public MqOutboxStore mqOutboxStore() {
        return new InMemoryMqOutboxStore();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MqOutboxStore.class)
    public MqOutboxPublisher mqOutboxPublisher(MqOutboxStore outboxStore,
                                               MqMessageCodec codec,
                                               MqRouteResolver routeResolver,
                                               DefaultMqHeaderResolver headerResolver) {
        return new DefaultMqOutboxPublisher(outboxStore, codec, routeResolver, headerResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({MqOutboxStore.class, RocketMQTemplate.class})
    public MqOutboxDispatcher mqOutboxDispatcher(MqOutboxStore outboxStore,
                                                 RocketMQTemplate rocketMQTemplate,
                                                 PeachRocketProperties properties) {
        return new MqOutboxDispatcher(outboxStore, rocketMQTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MqOutboxStore.class)
    public MqOutboxReplayService mqOutboxReplayService(MqOutboxStore outboxStore) {
        return new DefaultMqOutboxReplayService(outboxStore);
    }
}
