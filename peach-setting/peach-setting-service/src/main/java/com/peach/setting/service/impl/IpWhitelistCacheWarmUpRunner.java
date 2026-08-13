package com.peach.setting.service.impl;

import org.springframework.stereotype.Indexed;
import com.peach.setting.service.IIpWhitelistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * IP 白名单缓存预热任务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Slf4j
@Indexed
@Component
public class IpWhitelistCacheWarmUpRunner implements ApplicationRunner {

    private final IIpWhitelistService ipWhitelistService;

    public IpWhitelistCacheWarmUpRunner(IIpWhitelistService ipWhitelistService) {
        this.ipWhitelistService = ipWhitelistService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ipWhitelistService.warmUpCache();
            log.info("IP whitelist cache warm-up completed");
        } catch (Exception e) {
            log.warn("IP whitelist cache warm-up failed, reason={}", e.getClass().getSimpleName());
        }
    }
}
