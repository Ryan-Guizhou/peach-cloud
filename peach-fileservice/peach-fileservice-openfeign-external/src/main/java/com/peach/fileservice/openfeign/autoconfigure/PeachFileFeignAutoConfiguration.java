package com.peach.fileservice.openfeign.autoconfigure;


import com.peach.fileservice.openfeign.FileFeignClient;
import com.peach.fileservice.openfeign.fallback.FileFeignClientFallbackFactory;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Indexed;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 * @Description File服务feign自动装配
 */
@Slf4j
@Indexed
@AutoConfiguration
@ConditionalOnClass(FeignClient.class)
@EnableFeignClients(clients = FileFeignClient.class)
public class PeachFileFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FileFeignClientFallbackFactory fileFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        return new FileFeignClientFallbackFactory(fallbackSupport);
    }
}
