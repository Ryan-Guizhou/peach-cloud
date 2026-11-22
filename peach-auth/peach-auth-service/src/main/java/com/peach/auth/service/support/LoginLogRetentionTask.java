package com.peach.auth.service.support;

import com.peach.auth.service.impl.LoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
 * 每日按保留期分批清理；多实例可重复执行，单次最多100批以限制数据库负载。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LoginLogRetentionTask {
    private final LoginLogService service;
    /** 每日凌晨执行；每批独立事务，失败告警后由下一轮重试。 */
    @Scheduled(cron = "${peach.auth.login-audit.cleanup-cron:0 0 3 * * *}")
    public void clean() {
        try {
            for (int batch = 0; batch < 100; batch++) { if (service.deleteExpired() < 1000) return; }
            log.warn("Login audit retention batch limit reached; expired rows may remain until next run");
        }
        catch (RuntimeException ex) { log.error("Login audit retention failed, errorType={}", ex.getClass().getSimpleName()); }
    }
}
