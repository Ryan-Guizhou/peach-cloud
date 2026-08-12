package com.peach.message.openfeign.autoconfigure;

import com.peach.message.openfeign.MessageFeignClient;
import com.peach.message.openfeign.fallback.MessageFeignClientFallbackFactory;
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
 * @CreateTime 2026/6/23 14:45
 * @Description 消息服务Feign自动配置
 */
@Slf4j
@Indexed
@AutoConfiguration
@ConditionalOnClass(FeignClient.class)
@EnableFeignClients(clients = MessageFeignClient.class)
public class PeachMessageFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageFeignClientFallbackFactory messageFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        return new MessageFeignClientFallbackFactory(fallbackSupport);
    }
}
