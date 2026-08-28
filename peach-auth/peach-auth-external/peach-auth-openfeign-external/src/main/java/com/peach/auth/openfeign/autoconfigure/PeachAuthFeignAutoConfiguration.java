package com.peach.auth.openfeign.autoconfigure;

import com.peach.auth.openfeign.AuthFeignClient;
import com.peach.auth.openfeign.fallback.AuthFeignClientFallbackFactory;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Indexed;

import jakarta.annotation.PostConstruct;


/**
 * Auth服务feign自动装配。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 * @Description Auth服务feign自动装配
 */
@Slf4j
@Indexed
@AutoConfiguration
@ConditionalOnClass(FeignClient.class)
@EnableFeignClients(clients = AuthFeignClient.class)
public class PeachAuthFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthFeignClientFallbackFactory authFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        return new AuthFeignClientFallbackFactory(fallbackSupport);
    }

    @PostConstruct
    public void init() {
        log.info("AuthFeignClient has been inited");
    }
}
