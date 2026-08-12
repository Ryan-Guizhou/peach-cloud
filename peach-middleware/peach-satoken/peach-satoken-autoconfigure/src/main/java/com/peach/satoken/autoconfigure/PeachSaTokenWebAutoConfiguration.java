package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import com.peach.satoken.config.PeachSaTokenProperties;
import com.peach.satoken.filter.RequestIdFilter;
import com.peach.satoken.config.RequestIdProperties;
import com.peach.satoken.filter.UserContextFilter;
import com.peach.satoken.config.UserContextProperties;
import com.peach.satoken.security.SatokenEndpointMatcher;
import com.peach.satoken.support.UserContextSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token Servlet 自动配置。
 *
 * <p>负责注册业务服务侧 Same-Token 拦截器、请求 ID 过滤器和当前用户上下文恢复过滤器。
 * 该配置只在 Servlet Web 应用中生效，响应式网关不依赖该自动配置。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SaInterceptor.class, WebMvcConfigurer.class})
@EnableConfigurationProperties({PeachSaTokenProperties.class, RequestIdProperties.class,
        UserContextProperties.class})
public class PeachSaTokenWebAutoConfiguration {

    /**
     * 注册业务服务侧 Same-Token 拦截器。
     *
     * <p>公开端点与当前用户上下文过滤器使用同一套白名单；公开端点直接放行，
     * 非公开端点必须携带网关或可信服务注入的 Same-Token。</p>
     *
     * @param properties Sa-Token 扩展配置
     * @param userContextProperties 当前用户上下文与公开端点配置
     * @return MVC 拦截器配置
     */
    @Bean
    @ConditionalOnProperty(prefix = "peach.satoken.same-token", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public WebMvcConfigurer peachSaTokenWebMvcConfigurer(PeachSaTokenProperties properties,
                                                         UserContextProperties userContextProperties) {
        return new WebMvcConfigurer() {
            private final SatokenEndpointMatcher endpointMatcher = new SatokenEndpointMatcher();

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new SaInterceptor(handler -> {
                    String path = SaHolder.getRequest().getRequestPath();
                    String method = SaHolder.getRequest().getMethod();
                    if (properties.getSameToken().isLogPath()) {
                        log.debug("Sa-Token same-token check entered, method={}, path={}", method, path);
                    }
                    if (endpointMatcher.matches(userContextProperties.getPublicEndpoints(), method, path)) {
                        if (properties.getSameToken().isLogPath()) {
                            log.debug("Sa-Token same-token check skipped for public endpoint, method={}, path={}",
                                    method, path);
                        }
                        return;
                    }
                    SaSameUtil.checkCurrentRequestToken();
                })).addPathPatterns("/**")
                        .excludePathPatterns(properties.getSameToken().getExcludePathPatterns());
            }
        };
    }

    /**
     * 创建请求 ID 过滤器。
     *
     * @param properties 请求 ID 配置
     * @return 请求 ID 过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "peach.satoken.request-id", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RequestIdFilter requestIdFilter(RequestIdProperties properties) {
        return new RequestIdFilter(properties);
    }

    /**
     * 注册请求 ID 过滤器。
     *
     * @param filter 请求 ID 过滤器
     * @return Servlet 过滤器注册对象
     */
    @Bean
    @ConditionalOnBean(RequestIdFilter.class)
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(RequestIdFilter filter) {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("peachRequestIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    /**
     * 创建 Redis 用户上下文读取组件。
     *
     * @param stringRedisTemplate 字符串 Redis 模板
     * @return 用户上下文读取组件
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "peach.satoken.user-context", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public UserContextSupport userContextSupport(StringRedisTemplate stringRedisTemplate) {
        return new UserContextSupport(stringRedisTemplate);
    }

    /**
     * 创建当前用户上下文恢复过滤器。
     *
     * @param properties 当前用户上下文配置
     * @param userContextSupport 用户上下文读取组件
     * @return 当前用户上下文恢复过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(UserContextSupport.class)
    @ConditionalOnProperty(prefix = "peach.satoken.user-context", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public UserContextFilter userContextFilter(UserContextProperties properties, UserContextSupport userContextSupport) {
        return new UserContextFilter(properties, userContextSupport);
    }

    /**
     * 注册当前用户上下文恢复过滤器。
     *
     * @param filter 当前用户上下文恢复过滤器
     * @return Servlet 过滤器注册对象
     */
    @Bean
    @ConditionalOnBean(UserContextFilter.class)
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration(UserContextFilter filter) {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("peachUserContextFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 40);
        return registration;
    }
}
