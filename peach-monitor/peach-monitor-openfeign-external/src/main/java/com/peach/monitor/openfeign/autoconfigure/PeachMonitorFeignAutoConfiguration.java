package com.peach.monitor.openfeign.autoconfigure;


import com.peach.monitor.openfeign.MonitorFeignClient;
import com.peach.monitor.openfeign.fallback.MonitorFeignClientFallbackFactory;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 * @Description Monitor服务feign自动装配
 */
@AutoConfiguration
@ConditionalOnClass(FeignClient.class)
@EnableFeignClients(clients = MonitorFeignClient.class)
public class PeachMonitorFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MonitorFeignClientFallbackFactory monitorFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        return new MonitorFeignClientFallbackFactory(fallbackSupport);
    }
}
