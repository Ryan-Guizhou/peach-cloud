package com.peach.setting.service.impl;

import org.springframework.stereotype.Indexed;
import com.peach.setting.service.IIpWhitelistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * IpWhitelist缓存WarmUp运行器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class IpWhitelistCacheWarmUpRunner implements ApplicationRunner {

    private final IIpWhitelistService ipWhitelistService;

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
