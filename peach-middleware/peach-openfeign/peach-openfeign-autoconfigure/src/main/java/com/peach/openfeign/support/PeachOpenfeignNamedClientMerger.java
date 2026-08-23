package com.peach.openfeign.support;

import com.peach.openfeign.config.PeachOpenfeignProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.openfeign.FeignClientProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Peach OpenFeign 命名客户端配置合并器。
 *
 * <p>将 {@code peach.openfeign.client.named} 合并到 Spring Cloud OpenFeign 原生
 * {@link FeignClientProperties}，避免业务同时维护两套超时配置。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachOpenfeignNamedClientMerger implements InitializingBean {

    private final PeachOpenfeignProperties properties;

    private final FeignClientProperties feignClientProperties;

    public PeachOpenfeignNamedClientMerger(PeachOpenfeignProperties properties,
                                          FeignClientProperties feignClientProperties) {
        this.properties = properties;
        this.feignClientProperties = feignClientProperties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.getClient().getNamed() == null || properties.getClient().getNamed().isEmpty()) {
            return;
        }
        Map<String, FeignClientProperties.FeignClientConfiguration> config = feignClientProperties.getConfig();
        if (config == null) {
            config = new LinkedHashMap<String, FeignClientProperties.FeignClientConfiguration>();
            feignClientProperties.setConfig(config);
        }
        for (Map.Entry<String, PeachOpenfeignProperties.NamedClientOptions> entry
                : properties.getClient().getNamed().entrySet()) {
            mergeNamedClient(config, entry.getKey(), entry.getValue());
        }
    }

    private void mergeNamedClient(Map<String, FeignClientProperties.FeignClientConfiguration> config,
                                  String clientName,
                                  PeachOpenfeignProperties.NamedClientOptions options) {
        if (clientName == null || clientName.isBlank() || options == null) {
            return;
        }
        FeignClientProperties.FeignClientConfiguration clientConfig = config.get(clientName);
        if (clientConfig == null) {
            clientConfig = new FeignClientProperties.FeignClientConfiguration();
            config.put(clientName, clientConfig);
        }
        if (clientConfig.getConnectTimeout() == null && options.getConnectTimeoutMillis() != null) {
            clientConfig.setConnectTimeout(normalizeTimeout(options.getConnectTimeoutMillis()));
        }
        if (clientConfig.getReadTimeout() == null && options.getReadTimeoutMillis() != null) {
            clientConfig.setReadTimeout(normalizeTimeout(options.getReadTimeoutMillis()));
        }
    }

    private int normalizeTimeout(Integer timeoutMillis) {
        if (timeoutMillis == null) {
            return 1;
        }
        return Math.max(1, timeoutMillis);
    }
}
