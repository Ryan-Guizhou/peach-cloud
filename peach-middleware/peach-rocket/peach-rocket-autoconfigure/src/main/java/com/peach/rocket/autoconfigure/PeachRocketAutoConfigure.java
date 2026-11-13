package com.peach.rocket.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.rocket.codec.JacksonMqMessageCodec;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.codec.SecureJacksonMqMessageCodec;
import com.peach.rocket.consumer.DynamicRocketMqConsumerRegistrar;
import com.peach.rocket.consumer.MqConsumerInvoker;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.context.MqTraceContextPropagator;
import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.metrics.MqMetrics;
import com.peach.rocket.error.DefaultMqErrorHandler;
import com.peach.rocket.error.DefaultMqExceptionClassifier;
import com.peach.rocket.error.MqErrorHandler;
import com.peach.rocket.error.MqExceptionClassifier;
import com.peach.rocket.error.MqFailureRecorder;
import com.peach.rocket.idempotent.DefaultMqIdempotentKeyResolver;
import com.peach.rocket.idempotent.InMemoryMqIdempotentStore;
import com.peach.rocket.idempotent.MqIdempotentKeyResolver;
import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.producer.RocketMqPublisher;
import com.peach.rocket.route.AnnotationMqRouteResolver;
import com.peach.rocket.route.MqRouteResolver;
import com.peach.rocket.security.AesGcmMqPayloadEncryptor;
import com.peach.rocket.security.ConfigMqKeyProvider;
import com.peach.rocket.security.ConfigurableMqEncryptionPolicy;
import com.peach.rocket.security.MqEncryptionPolicy;
import com.peach.rocket.security.MqKeyProvider;
import com.peach.rocket.security.MqPayloadEncryptor;
import com.peach.rocket.topic.RocketMqTopicAdmin;
import com.peach.rocket.transaction.RocketMqTransactionMessageProducer;
import com.peach.rocket.core.MqTransactionHandler;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * RocketMQ 核心自动装配：路由、编解码、幂等、消费与 Producer 均支持业务覆盖 SPI。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@AutoConfigureAfter({RocketMQAutoConfiguration.class,RocketMQProperties.class})
@ConditionalOnClass(RocketMQTemplate.class)
@EnableConfigurationProperties(PeachRocketProperties.class)
@ConditionalOnProperty(prefix = "peach.rocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PeachRocketAutoConfigure {

    /** 在原生 Producer 构造前应用统一安全配置，避免模板与独立客户端身份不一致。 */
    @Bean
    public static org.springframework.beans.factory.config.BeanPostProcessor rocketClientSecurityProperties(
            ObjectProvider<PeachRocketProperties> properties) {
        return new org.springframework.beans.factory.config.BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (bean instanceof RocketMQProperties rocket) {
                    com.peach.rocket.support.MqClientSecurity.configureProperties(rocket, properties.getObject());
                }
                return bean;
            }
        };
    }

    /** 生产模式及显式可靠性模式都必须验证真实 Store 能力。 */
    @Bean
    public SmartInitializingSingleton rocketProductionVerifier(PeachRocketProperties properties,
            MqIdempotentStore store, ObjectProvider<MqKeyProvider> keys) {
        return () -> {
            if ((properties.isProduction() || properties.getConsumer().isRequireDurableIdempotency())
                    && (!store.isDurable() || !store.supportsOwnerFencing() || !store.supportsLeaseRenewal())) {
                throw new IllegalStateException("RocketMQ requires a durable token-fenced idempotent store");
            }
            if (properties.getSecurity().isEnabled()) {
                MqKeyProvider provider = keys.getIfAvailable();
                if (provider == null) {
                    throw new IllegalStateException("MQ security requires a key provider");
                }
                provider.getKey(properties.getSecurity().getKeyId());
            }
        };
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public com.peach.rocket.consumer.MqLeaseRenewer mqLeaseRenewer(MqIdempotentStore store, PeachRocketProperties properties) {
        return new com.peach.rocket.consumer.MqLeaseRenewer(store, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "peach.rocket.security", name = "enabled", havingValue = "true")
    public MqKeyProvider mqKeyProvider(PeachRocketProperties properties) {
        return new ConfigMqKeyProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "peach.rocket.security", name = "enabled", havingValue = "true")
    public MqPayloadEncryptor mqPayloadEncryptor(MqKeyProvider keyProvider) {
        return new AesGcmMqPayloadEncryptor(keyProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "peach.rocket.security", name = "enabled", havingValue = "true")
    public MqEncryptionPolicy mqEncryptionPolicy(PeachRocketProperties properties) {
        return new ConfigurableMqEncryptionPolicy(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqMessageCodec mqMessageCodec(ObjectMapper objectMapper,
                                         ObjectProvider<MqPayloadEncryptor> encryptorProvider,
                                         ObjectProvider<MqEncryptionPolicy> encryptionPolicyProvider,
                                         PeachRocketProperties properties) {
        if (!properties.getSecurity().isEnabled()) {
            return new JacksonMqMessageCodec(objectMapper, properties);
        }
        return new SecureJacksonMqMessageCodec(objectMapper, encryptorProvider.getIfAvailable(),
                                               encryptionPolicyProvider.getIfAvailable(), properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqRouteResolver mqRouteResolver(PeachRocketProperties properties) {
        return new AnnotationMqRouteResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultMqHeaderResolver defaultMqHeaderResolver(
            ObjectProvider<MqTraceContextPropagator> traceContextPropagatorProvider) {
        return new DefaultMqHeaderResolver(traceContextPropagatorProvider.getIfAvailable(
                () -> MqTraceContextPropagator.NOOP));
    }

    @Bean
    @ConditionalOnMissingBean
    public MqIdempotentStore mqIdempotentStore(PeachRocketProperties properties) {
        return new InMemoryMqIdempotentStore(properties.getConsumer().getInMemoryMaxEntries());
    }

    /**
     * 关键业务可要求跨实例、带 owner fencing 的幂等实现，启动时阻止退回内存实现。
     *
     * @param store 已装配的幂等存储
     * @return 启动验证回调
     */
    @Bean
    @ConditionalOnProperty(prefix = "peach.rocket.consumer", name = "require-durable-idempotency",
            havingValue = "true")
    public SmartInitializingSingleton rocketIdempotencyDurabilityVerifier(MqIdempotentStore store) {
        return () -> {
            if (store instanceof InMemoryMqIdempotentStore || !store.supportsOwnerFencing()) {
                throw new IllegalStateException(
                        "peach.rocket.consumer.require-durable-idempotency requires a durable token-fenced store");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public MqIdempotentKeyResolver mqIdempotentKeyResolver(PeachRocketProperties properties) {
        return new DefaultMqIdempotentKeyResolver(properties.getConsumer().isLegacyIdempotentKey());
    }

    @Bean
    @ConditionalOnMissingBean
    public MqErrorHandler mqErrorHandler() {
        return new DefaultMqErrorHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public MqExceptionClassifier mqExceptionClassifier() {
        return new DefaultMqExceptionClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    public MqConsumerInvoker mqConsumerInvoker(MqMessageCodec codec,
                                               MqIdempotentStore idempotentStore,
                                               MqIdempotentKeyResolver idempotentKeyResolver,
                                               MqErrorHandler errorHandler,
                                               MqExceptionClassifier exceptionClassifier,
                                               PeachRocketProperties properties,
                                               ObjectProvider<MqTraceContextPropagator> traceContextPropagatorProvider,
                                               ObjectProvider<MqFailureRecorder> failureRecorderProvider,
                                               ObjectProvider<MqMetrics> metricsProvider,
                                               com.peach.rocket.consumer.MqLeaseRenewer leaseRenewer) {
        return new MqConsumerInvoker(codec, idempotentStore, idempotentKeyResolver,
                                     errorHandler, exceptionClassifier, properties,
                                     traceContextPropagatorProvider.getIfAvailable(() -> MqTraceContextPropagator.NOOP),
                                     failureRecorderProvider.getIfAvailable(() -> MqFailureRecorder.NOOP),
                                     metricsProvider.getIfAvailable(() -> MqMetrics.NOOP), leaseRenewer);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "peach.rocket.consumer", name = "dynamic-register", havingValue = "true", matchIfMissing = true)
    public DynamicRocketMqConsumerRegistrar dynamicRocketMqConsumerRegistrar(RocketMQProperties rocketMQProperties,
                                                                            PeachRocketProperties properties,
                                                                            MqConsumerInvoker invoker,
                                                                            ObjectProvider<MqMetrics> metricsProvider) {
        return new DynamicRocketMqConsumerRegistrar(rocketMQProperties, properties, invoker,
                metricsProvider.getIfAvailable(() -> MqMetrics.NOOP));
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.apache.rocketmq.tools.admin.DefaultMQAdminExt")
    @ConditionalOnProperty(prefix = "peach.rocket.topic", name = "auto-create", havingValue = "true")
    public RocketMqTopicAdmin rocketMqTopicAdmin(RocketMQProperties rocketMQProperties,
                                                 PeachRocketProperties properties,
                                                 ApplicationContext applicationContext) {
        return new RocketMqTopicAdmin(rocketMQProperties, properties, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "peach.rocket.transaction", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RocketMqTransactionMessageProducer rocketMqTransactionMessageProducer(RocketMQProperties rocketMQProperties,
                                                                                 PeachRocketProperties properties,
                                                                                 MqMessageCodec codec,
                                                                                 MqRouteResolver routeResolver,
                                                                                 DefaultMqHeaderResolver headerResolver,
                                                                                 ObjectProvider<MqTransactionHandler<?>> transactionHandlers) {
        return new RocketMqTransactionMessageProducer(rocketMQProperties, properties, codec,
                                                      routeResolver, headerResolver, transactionHandlers);
    }

    @Bean
    @ConditionalOnMissingBean
    public MqPublisher mqPublisher(RocketMQTemplate rocketMQTemplate,
                                   MqMessageCodec codec,
                                   MqRouteResolver routeResolver,
                                   DefaultMqHeaderResolver headerResolver,
                                   PeachRocketProperties properties,
                                   ObjectProvider<RocketMqTransactionMessageProducer> transactionProducerProvider,
                                   ObjectProvider<MqMetrics> metricsProvider) {
        return new RocketMqPublisher(rocketMQTemplate, codec, routeResolver, headerResolver,
                                     properties, transactionProducerProvider.getIfAvailable(),
                                     metricsProvider.getIfAvailable(() -> MqMetrics.NOOP));
    }
}
