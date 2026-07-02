package com.peach.auth.openfeign.autoconfigure;

import com.peach.auth.openfeign.UserFeignClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 * @Description Auth服务feign自动装配
 */
@AutoConfiguration
@ConditionalOnClass(FeignClient.class)
@EnableFeignClients(clients = UserFeignClient.class)
public class PeachAuthFeignAutoConfiguration {
}
