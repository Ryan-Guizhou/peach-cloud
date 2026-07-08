package com.peach.monitor.openfeign.autoconfigure;


import com.peach.monitor.openfeign.MonitorFeignClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 * @Description Monitor服务feign自动装配
 */
@AutoConfiguration
@ConditionalOnClass(FeignClient.class)
@EnableFeignClients(clients = MonitorFeignClient.class)
public class PeachAuthFeignMonitorConfiguration {
}
