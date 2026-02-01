package com.peach.fileservice.openfeign;

import cn.dev33.satoken.same.SaSameUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 * @Description Feign 配置：添加 RequestInterceptor 以透传 Sa-Token
 */
@Slf4j
@Indexed
@Configuration
public class FeignConfigure {

    @Autowired
    private ObjectFactory<HttpMessageConverters> messageConverters;

    @Bean
    public Encoder feignFormEncoder() {
        // 支持 multipart/form-data，必须包装 SpringEncoder 才能正确处理 MultipartFile
        return new SpringFormEncoder(new SpringEncoder(messageConverters));
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 从当前请求上下文中获取原始请求
                ServletRequestAttributes attributes =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // 复制请求头，但排除敏感或冲突的头信息
                    // 排除 Content-Type, Content-Length, Host 等
                    List<String> excludeHeaders = Arrays.asList("content-type", "content-length", "host");
                    
                    Enumeration<String> headerNames = request.getHeaderNames();
                    while (headerNames.hasMoreElements()) {
                        String headerName = headerNames.nextElement();
                        if (excludeHeaders.contains(headerName.toLowerCase())) {
                            continue;
                        }
                        String headerValue = request.getHeader(headerName);
                        template.header(headerName, headerValue);
                    }
                }
                // 注入 Same-Token，确保内部服务调用通过鉴权
                template.header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken());
            }
        };
    }
}
