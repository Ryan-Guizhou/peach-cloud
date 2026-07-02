package com.peach.satoken.web.autoconfigure;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import com.peach.satoken.config.PeachSaTokenProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({SaInterceptor.class, WebMvcConfigurer.class})
@ConditionalOnProperty(prefix = "peach.satoken.same-token", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachSaTokenProperties.class)
public class PeachSaTokenWebAutoConfiguration {

    @Bean
    public WebMvcConfigurer peachSaTokenWebMvcConfigurer(PeachSaTokenProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new SaInterceptor(handler -> {
                    String path = SaHolder.getRequest().getRequestPath();
                    if (properties.getSameToken().isLogPath()) {
                        log.info("Sa-Token Same-Token check entering path: {}", path);
                    }
                    SaSameUtil.checkCurrentRequestToken();
                })).addPathPatterns("/**")
                        .excludePathPatterns(properties.getSameToken().getExcludePathPatterns());
            }
        };
    }
}
