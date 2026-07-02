package com.peach.openfeign.autoconfigure;

import cn.dev33.satoken.same.SaSameUtil;
import com.peach.openfeign.config.PeachOpenfeignProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;


/**
 * OpenFeign 自动配置类
 * <p>
 * 作用：
 * 1. 自动注入 Feign 请求拦截器
 * 2. 支持请求头透传（Header Relay）
 * 3. 支持 Sa-Token Same-Token 跨服务安全透传
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/2 14:11
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
@ConditionalOnProperty(
        prefix = "peach.openfeign",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(PeachOpenfeignProperties.class)
public class PeachOpenfeignAutoConfiguration {


    /**
     * Feign 请求拦截器
     * <p>
     * 功能：
     * 1. 透传当前请求 Header
     * 2. 注入 Same-Token（跨服务认证）
     */
    @Bean("peachOpenfeignRequestInterceptor")
    @ConditionalOnMissingBean(name = "peachOpenfeignRequestInterceptor")
    public RequestInterceptor peachOpenfeignRequestInterceptor(
            PeachOpenfeignProperties properties) {

        log.info("[PeachFeign] OpenFeign interceptor initialized. relayHeaders={}, sameTokenEnabled={}",
                properties.isRelayHeaders(),
                properties.isSameTokenEnabled());

        return template -> {

            log.debug("[PeachFeign] Start processing Feign request: {}", template.url());

            relayHeaders(template, properties);
            addSameToken(template, properties);

            log.debug("[PeachFeign] Finished processing Feign request: {}", template.url());
        };
    }

    /**
     * 透传当前 HTTP 请求中的 Header 到 Feign 请求
     *
     * @param template   Feign 请求模板
     * @param properties 配置属性
     */
    private void relayHeaders(RequestTemplate template,
                              PeachOpenfeignProperties properties) {

        if (!properties.isRelayHeaders()) {
            log.debug("[PeachFeign] Header relay disabled, skip relay step");
            return;
        }

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            log.debug("[PeachFeign] No ServletRequestAttributes found, skip header relay");
            return;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = attributes.getRequest();

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            log.debug("[PeachFeign] No request headers found");
            return;
        }

        int relayCount = 0;

        while (headerNames.hasMoreElements()) {

            String headerName = headerNames.nextElement();

            // 排除 header
            if (isExcluded(headerName, properties)) {
                log.debug("[PeachFeign] Excluded header: {}", headerName);
                continue;
            }

            if (SaSameUtil.SAME_TOKEN.equalsIgnoreCase(headerName)) {
                continue;
            }

            List<String> headerValues = Collections.list(request.getHeaders(headerName));
            if (headerValues.isEmpty()) {
                continue;
            }

            template.header(headerName, headerValues.toArray(new String[0]));
            relayCount ++;
        }

        log.debug("[PeachFeign] Header relay completed, count={}", relayCount);
    }

    /**
     * 添加 Sa-Token Same-Token
     * <p>
     * 用于微服务之间身份一致性校验
     *
     * @param template   Feign 请求模板
     * @param properties 配置
     */
    private void addSameToken(RequestTemplate template,
                              PeachOpenfeignProperties properties) {

        if (!properties.isSameTokenEnabled()) {
            log.debug("[PeachFeign] SameToken disabled");
            return;
        }

        String token = SaSameUtil.getToken();

        if (token == null || token.isEmpty()) {
            log.warn("[PeachFeign] SameToken is enabled but token is null");
            return;
        }

        template.removeHeader(SaSameUtil.SAME_TOKEN);
        template.header(SaSameUtil.SAME_TOKEN, token);

        log.debug("[PeachFeign] SameToken injected successfully");
    }

    /**
     * 判断 Header 是否需要被排除
     *
     * @param headerName 当前 header 名称
     * @param properties  配置
     * @return true = 排除
     */
    private boolean isExcluded(String headerName,
                               PeachOpenfeignProperties properties) {

        if (properties.getExcludeHeaders() == null
                || properties.getExcludeHeaders().isEmpty()) {
            return false;
        }

        String lowerHeaderName = headerName.toLowerCase(Locale.ROOT);

        boolean excluded = properties.getExcludeHeaders()
                .stream()
                .map(h -> h.toLowerCase(Locale.ROOT))
                .anyMatch(lowerHeaderName::equals);

        if (excluded) {
            log.debug("[PeachFeign] Header excluded by config: {}", headerName);
        }

        return excluded;
    }
}