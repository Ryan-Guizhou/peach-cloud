package com.peach.message.openfeign.autoconfigure;

import com.peach.message.openfeign.MessageFeignClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息服务Feign自动配置
 */
@AutoConfiguration
@ConditionalOnClass(FeignClient.class)
@EnableFeignClients(clients = MessageFeignClient.class)
public class PeachMessageFeignAutoConfiguration {
}
