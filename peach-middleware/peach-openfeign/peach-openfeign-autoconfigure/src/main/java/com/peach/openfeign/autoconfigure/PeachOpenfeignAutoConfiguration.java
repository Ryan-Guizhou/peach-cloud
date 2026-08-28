package com.peach.openfeign.autoconfigure;

import com.peach.openfeign.config.PeachOpenfeignProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Indexed;

import jakarta.annotation.PostConstruct;


/**
 * PeachOpenFeign自动配置。
 * <p>
 * 该类保持最小职责：仅在启动阶段输出传输层与熔断治理的关键运行态，
 * 便于快速核对配置项与依赖是否一致，不承载具体拦截、重试或异常治理逻辑。
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
@ConditionalOnClass(name = "feign.RequestInterceptor")
@ConditionalOnProperty(prefix = "peach.openfeign", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachOpenfeignProperties.class)
public class PeachOpenfeignAutoConfiguration {

    private final PeachOpenfeignProperties properties;

    private final Environment environment;

    public PeachOpenfeignAutoConfiguration(PeachOpenfeignProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /**
     * 在应用启动后输出传输层与熔断治理的运行态信息。
     */
    @PostConstruct
    public void logClientTransport() {
        if (!properties.getClient().isOkhttpEnabled()) {
            log.info("[PeachFeign] OkHttp disabled by peach.openfeign.client.okhttp-enabled=false");
            return;
        }
        boolean okhttpOnClasspath = isPresent("feign.okhttp.OkHttpClient");
        boolean feignOkhttpEnabled = environment.getProperty("feign.okhttp.enabled", Boolean.class, Boolean.TRUE);
        if (okhttpOnClasspath && feignOkhttpEnabled) {
            log.info("[PeachFeign] OkHttp client enabled");
            return;
        }
        if (!okhttpOnClasspath) {
            log.warn("[PeachFeign] feign-okhttp not on classpath, fallback to default HTTP client");
        }
        if (!feignOkhttpEnabled) {
            log.info("[PeachFeign] feign.okhttp.enabled=false, using default HTTP client");
        }
    }

    private boolean isPresent(String className) {
        try {
            Class.forName(className, false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
