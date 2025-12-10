package com.peach.redis.bloom.autoconfigure;


import com.peach.redis.common.RedisConfig;
import com.peach.redis.bloom.config.BloomFilterProperties;
import com.peach.redis.bloom.constant.SpiConstant;
import com.peach.redis.bloom.core.BloomFilterService;
import com.peach.redis.bloom.core.SegmentedBloomFilterService;
import com.peach.redis.bloom.spi.BloomScalePolicy;
import com.peach.redis.bloom.spi.CodecProvider;
import com.peach.redis.bloom.spi.KeyNamingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 自动装配：在容器中存在 {@link org.redisson.api.RedissonClient} 时，
 * 提供默认的 {@link com.peach.redis.bloom.core.BloomFilterService} 实现。
 * 支持通过 SPI 覆盖命名策略、编解码器与扩容策略；
 * 同时允许用户自定义 Bean 覆盖（使用 @ConditionalOnMissingBean）。
 */
@Slf4j
@Configuration
@AutoConfigureAfter(RedisConfig.class)
@EnableConfigurationProperties(BloomFilterProperties.class)
@ConditionalOnProperty(prefix = "peach.redis.bloom", name = "enabled", matchIfMissing = true)
public class BloomFilterAutoConfiguration {


    @Bean
    @Primary
    @DependsOn("redissonClient")
    @ConditionalOnMissingBean(BloomFilterService.class)
    public BloomFilterService bloomFilterService(RedissonClient redissonClient,
                                                 BloomFilterProperties properties) {

        log.info("🎉 Starting BloomFilterService auto-configuration...");

        // 使用增强的SPI加载方法
        CodecProvider codecProvider = loadSpiImplementation(CodecProvider.class,
                CodecProvider.defaultJacksonCodec(), SpiConstant.CODEC_SPI_NAME);

        KeyNamingStrategy keyNaming = loadSpiImplementation(KeyNamingStrategy.class,
                KeyNamingStrategy.defaultStrategy(), SpiConstant.KEY_NAMING_SPI_NAME);

        BloomScalePolicy scalePolicy = loadSpiImplementation(BloomScalePolicy.class,
                BloomScalePolicy.defaultPolicy(), SpiConstant.SCALE_POLICY_SPI_NAME);

        // 汇总报告
        log.info("🎊 BloomFilterService Configuration Summary:");
        log.info("   📊 Codec Provider: {}", getImplementationName(codecProvider));
        log.info("   🔑 Key Naming: {}", getImplementationName(keyNaming));
        log.info("   📈 Scale Policy: {}", getImplementationName(scalePolicy));

        return new SegmentedBloomFilterService(redissonClient, properties,
                codecProvider, keyNaming, scalePolicy);
    }


    /**
     * 获取实现名称的统一方法
     */
    private <T> String getImplementationName(T implementation) {
        String className = implementation.getClass().getSimpleName();

        if (implementation instanceof CodecProvider) {
            return String.format("%s(%s)", className, ((CodecProvider) implementation).getName());
        } else if (implementation instanceof KeyNamingStrategy) {
            return String.format("%s(%s)", className, ((KeyNamingStrategy) implementation).getName());
        } else if (implementation instanceof BloomScalePolicy) {
            return String.format("%s(%s)", className, ((BloomScalePolicy) implementation).getName());
        }

        return className;
    }

    /**
     * 完整的SPI加载方法，包含详细日志
     */
    private <T> T loadSpiImplementation(Class<T> spiClass, T defaultImpl, String spiName) {
        log.info("=================================================================");
        log.info("🔄 SPI Loading: {}", spiName);
        log.info("=================================================================");

        long startTime = System.currentTimeMillis();
        int implementationCount = 0;

        try {
            ServiceLoader<T> loader = ServiceLoader.load(spiClass);
            List<T> implementations = new ArrayList<>();
            List<String> implementationNames = new ArrayList<>();

            // 遍历所有实现
            for (T implementation : loader) {
                implementations.add(implementation);
                String implName = getImplementationName(implementation);
                implementationNames.add(implName);
                implementationCount++;

                log.info("📍 Discovered implementation: {}", implName);
            }

            long loadTime = System.currentTimeMillis() - startTime;

            if (implementations.isEmpty()) {
                log.warn("❌ SPI LOAD RESULT: No custom implementation found for {}", spiName);
                log.info("📋 Falling back to DEFAULT implementation: {}", getImplementationName(defaultImpl));
                log.info("⏱️  Loading time: {}ms", loadTime);
                log.info("=================================================================");
                return defaultImpl;
            } else {
                T selected = implementations.get(0);
                String selectedName = getImplementationName(selected);

                log.info("✅ SPI LOAD RESULT: Successfully loaded {} implementation(s)", implementationCount);
                log.info("🎯 Selected implementation: {}", selectedName);
                if (implementationCount > 1) {
                    log.info("📜 All available implementations: {}", implementationNames);
                }
                log.info("⏱️  Loading time: {}ms", loadTime);
                log.info("=================================================================");

                return selected;
            }

        } catch (Exception e) {
            log.error("💥 ERROR loading SPI implementation for {}: {}", spiName, e.getMessage(), e);
            log.warn("🔄 Falling back to default implementation due to error");
            return defaultImpl;
        }
    }
}