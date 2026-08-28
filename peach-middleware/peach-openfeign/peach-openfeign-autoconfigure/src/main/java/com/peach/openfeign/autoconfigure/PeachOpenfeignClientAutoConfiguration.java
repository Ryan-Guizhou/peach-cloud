package com.peach.openfeign.autoconfigure;

import com.peach.openfeign.config.PeachOpenfeignProperties;
import com.peach.openfeign.support.PeachOpenFeignErrorDecoder;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import com.peach.openfeign.support.PeachFeignFallbackValidator;
import com.peach.openfeign.support.PeachOpenfeignRetryPolicy;
import com.peach.openfeign.support.PeachOpenfeignRetryer;
import com.peach.openfeign.support.PeachOpenfeignRequestInterceptor;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Indexed;

import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * PeachOpenFeign客户端自动配置。
 * <p>
 * 负责装配超时、错误解码、有界重试、Same-Token 与 RequestId 注入、fallback 校验能力，
 * 为所有 Feign 客户端提供统一治理基线。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
@Slf4j
@Indexed
@AutoConfiguration
@AutoConfigureBefore(FeignAutoConfiguration.class)
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnProperty(prefix = "peach.openfeign", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachOpenfeignProperties.class)
public class PeachOpenfeignClientAutoConfiguration {

    /**
     * 创建 Feign 全局默认超时配置。
     *
     * @param properties openfeign 模块配置
     * @return Feign 请求选项
     */
    @Bean
    @ConditionalOnMissingBean
    public Request.Options peachFeignRequestOptions(PeachOpenfeignProperties properties) {
        PeachOpenfeignProperties.ClientOptions client = properties.getClient();
        int connectTimeoutMillis = normalizeTimeout(client.getConnectTimeoutMillis());
        int readTimeoutMillis = normalizeTimeout(client.getReadTimeoutMillis());
        log.info("[PeachFeign] default timeout connect={}ms read={}ms followRedirects={}",
                connectTimeoutMillis,
                readTimeoutMillis,
                client.isFollowRedirects());
        return new Request.Options(
                connectTimeoutMillis,
                TimeUnit.MILLISECONDS,
                readTimeoutMillis,
                TimeUnit.MILLISECONDS,
                client.isFollowRedirects()
        );
    }

    /**
     * 创建统一错误解码器，用于标准化下游异常语义。
     *
     * @param properties openfeign 模块配置
     * @return Feign 错误解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public ErrorDecoder peachFeignErrorDecoder(PeachOpenfeignProperties properties) {
        return new PeachOpenFeignErrorDecoder(new PeachOpenfeignRetryPolicy(properties));
    }

    /**
     * 创建有界重试器，仅对策略允许的方法与状态执行重试。
     *
     * @param properties openfeign 模块配置
     * @return Feign 重试器
     */
    @Bean
    @ConditionalOnMissingBean
    public Retryer peachFeignRetryer(PeachOpenfeignProperties properties) {
        PeachOpenfeignRetryPolicy retryPolicy = new PeachOpenfeignRetryPolicy(properties);
        log.info("[PeachFeign] retry initialized enabled={} maxAttempts={} methods={} statuses={}",
                retryPolicy.isEnabled(),
                retryPolicy.getMaxAttempts(),
                retryPolicy.getMethods(),
                retryPolicy.getStatuses());
        return new PeachOpenfeignRetryer(retryPolicy);
    }

    /**
     * 创建请求拦截器，负责 Same-Token 与 RequestId 注入。
     *
     * @param properties openfeign 模块配置
     * @return Feign 请求拦截器
     */
    @Bean("peachOpenfeignRequestInterceptor")
    @ConditionalOnMissingBean(name = "peachOpenfeignRequestInterceptor")
    public RequestInterceptor peachOpenfeignRequestInterceptor(PeachOpenfeignProperties properties) {
        log.info("[PeachFeign] interceptor initialized sameTokenEnabled={} sameTokenFailFast={} requestIdEnabled={}",
                properties.isSameTokenEnabled(),
                properties.isSameTokenFailFast(),
                properties.isRequestIdEnabled());
        return new PeachOpenfeignRequestInterceptor(properties);
    }

    /**
     * 创建业务 fallbackFactory 可复用的辅助支持对象。
     *
     * @return fallback 支持组件
     */
    @Bean
    @ConditionalOnMissingBean
    public PeachFeignFallbackSupport peachFeignFallbackSupport() {
        return new PeachFeignFallbackSupport();
    }

    /**
     * 创建 fallback 启动校验器，检测是否缺少 fallback/fallbackFactory。
     *
     * @param properties openfeign 模块配置
     * @param environment Spring 环境
     * @param feignClients 已发现的 Feign 客户端
     * @return fallback 校验器
     */
    @Bean
    @ConditionalOnClass(FeignClientFactoryBean.class)
    @ConditionalOnMissingBean
    public PeachFeignFallbackValidator peachFeignFallbackValidator(PeachOpenfeignProperties properties,
                                                                  Environment environment,
                                                                  Map<String, FeignClientFactoryBean> feignClients) {
        return new PeachFeignFallbackValidator(properties, environment, feignClients);
    }

    private int normalizeTimeout(int timeoutMillis) {
        return Math.max(1, timeoutMillis);
    }
}
