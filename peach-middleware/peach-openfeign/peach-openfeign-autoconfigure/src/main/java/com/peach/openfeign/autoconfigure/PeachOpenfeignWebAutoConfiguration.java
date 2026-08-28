package com.peach.openfeign.autoconfigure;

import com.peach.openfeign.config.PeachOpenfeignProperties;
import com.peach.openfeign.support.PeachOpenfeignNamedClientMerger;
import com.peach.openfeign.web.PeachOpenfeignExceptionHandler;
import com.peach.openfeign.web.PeachOpenfeignSentinelExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Indexed;

/**
 * PeachOpenFeignWeb自动配置。
 * <p>负责导入统一异常处理器，并在启动期将
 * {@code peach.openfeign.client.named} 的命名客户端参数合并到
 * {@link FeignClientProperties}，保证配置入口一致。</p>
 * <p>该配置仅在 Servlet Web 应用生效，不作用于 Gateway WebFlux 场景。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
@Indexed
@AutoConfiguration
@AutoConfigureAfter(PeachOpenfeignClientAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "peach.openfeign", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachOpenfeignProperties.class)
@Import({PeachOpenfeignExceptionHandler.class, PeachOpenfeignSentinelExceptionHandler.class})
public class PeachOpenfeignWebAutoConfiguration {

    /**
     * 创建命名客户端配置合并器。
     *
     * @param properties openfeign 模块配置
     * @param feignClientProperties Spring Cloud Feign 客户端配置
     * @return 命名客户端配置合并器
     */
    @Bean
    @ConditionalOnClass(FeignClientProperties.class)
    @ConditionalOnMissingBean
    public PeachOpenfeignNamedClientMerger peachOpenfeignNamedClientMerger(
            PeachOpenfeignProperties properties,
            FeignClientProperties feignClientProperties) {
        return new PeachOpenfeignNamedClientMerger(properties, feignClientProperties);
    }
}
