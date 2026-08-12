package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import com.peach.satoken.config.PeachSaTokenProperties;
import com.peach.satoken.filter.RequestIdFilter;
import com.peach.satoken.config.RequestIdProperties;
import com.peach.satoken.filter.UserContextFilter;
import com.peach.satoken.config.UserContextProperties;
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
 * Servlet auto-configuration for Same-Token, request ids and current-user context.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SaInterceptor.class, WebMvcConfigurer.class})
@EnableConfigurationProperties({PeachSaTokenProperties.class, RequestIdProperties.class,
        UserContextProperties.class})
public class PeachSaTokenWebAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "peach.satoken.same-token", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public WebMvcConfigurer peachSaTokenWebMvcConfigurer(PeachSaTokenProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new SaInterceptor(handler -> {
                    String path = SaHolder.getRequest().getRequestPath();
                    if (properties.getSameToken().isLogPath()) {
                        log.debug("Sa-Token Same-Token check entering path: {}", path);
                    }
                    SaSameUtil.checkCurrentRequestToken();
                })).addPathPatterns("/**")
                        .excludePathPatterns(properties.getSameToken().getExcludePathPatterns());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "peach.satoken.request-id", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RequestIdFilter requestIdFilter(RequestIdProperties properties) {
        return new RequestIdFilter(properties);
    }

    @Bean
    @ConditionalOnBean(RequestIdFilter.class)
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(RequestIdFilter filter) {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("peachRequestIdFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }



    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "peach.satoken.user-context", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public UserContextSupport userContextSupport(StringRedisTemplate stringRedisTemplate) {
        return new UserContextSupport(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(UserContextSupport.class)
    @ConditionalOnProperty(prefix = "peach.satoken.user-context", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public UserContextFilter userContextFilter(UserContextProperties properties, UserContextSupport userContextSupport) {
        return new UserContextFilter(properties, userContextSupport);
    }

    @Bean
    @ConditionalOnBean(UserContextFilter.class)
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration(UserContextFilter filter) {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("peachUserContextFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 40);
        return registration;
    }
}
