package com.peach.setting.openfeign.autoconfigure;


import com.peach.openfeign.support.PeachFeignFallbackSupport;
import com.peach.setting.openfeign.SettingFeignClient;
import com.peach.setting.openfeign.fallback.SettingFeignClientFallbackFactory;
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
 * @Description Setting服务feign自动装配
 */
@AutoConfiguration
@ConditionalOnClass(FeignClient.class)
@EnableFeignClients(clients = SettingFeignClient.class)
public class PeachSettingFeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SettingFeignClientFallbackFactory settingFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        return new SettingFeignClientFallbackFactory(fallbackSupport);
    }
}
