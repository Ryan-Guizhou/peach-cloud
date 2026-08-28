package com.peach.observability.autoconfigure;

import com.peach.observability.config.PeachObservabilityProperties;
import com.peach.observability.core.RequestIdResolver;
import com.peach.observability.web.RequestIdServletFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * PeachServletObservability自动配置。
 * <p>该配置只在 Servlet 应用中注册请求 ID 过滤器，并早于认证与用户上下文过滤器执行。
 * 响应式网关不加载 Servlet API，也不会创建该过滤器。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@AutoConfiguration
@AutoConfigureAfter(PeachObservabilityAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({Filter.class, RequestIdServletFilter.class})
@ConditionalOnProperty(prefix = "peach.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PeachServletObservabilityAutoConfiguration {

    /**
     * 创建 Servlet 请求 ID 过滤器。
     *
     * @param properties 可观测性配置
     * @param resolver 请求 ID 解析器
     * @return 请求 ID 过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "peach.observability.request-id", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RequestIdServletFilter peachRequestIdServletFilter(PeachObservabilityProperties properties,
                                                              RequestIdResolver resolver) {
        return new RequestIdServletFilter(properties.getRequestId(), resolver);
    }

    /**
     * 注册请求 ID 过滤器并设置为高优先级。
     *
     * @param filter 请求 ID 过滤器
     * @return Servlet 过滤器注册对象
     */
    @Bean
    @ConditionalOnMissingBean(name = "peachRequestIdServletFilterRegistration")
    public FilterRegistrationBean<RequestIdServletFilter> peachRequestIdServletFilterRegistration(
            RequestIdServletFilter filter) {
        FilterRegistrationBean<RequestIdServletFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("peachRequestIdServletFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
