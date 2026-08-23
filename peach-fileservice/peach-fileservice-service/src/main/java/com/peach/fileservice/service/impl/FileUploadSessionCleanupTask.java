package com.peach.fileservice.service.impl;

import lombok.RequiredArgsConstructor;

import com.peach.fileservice.config.FileDomainProperties;
import com.peach.fileservice.service.IFileDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 过期上传会话清理定时任务
 *
 * <p>定期清理超时未完成的分片上传会话，释放临时资源，避免存储泄漏。
 * 清理频率通过 {@link FileDomainProperties#getUploadSessionCleanupCron()} 配置。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Slf4j
@Indexed
@Component
@EnableScheduling
@RequiredArgsConstructor
public class FileUploadSessionCleanupTask {

        private final IFileDomainService fileDomainService;

        private final FileDomainProperties fileDomainProperties;

    /**
     * 执行过期上传会话清理
     *
     * <p>根据配置的 Cron 表达式定期执行，
     * 清理所有超过过期时间的上传会话及其已上传的分片数据。</p>
     */
    @Scheduled(cron = "#{@fileDomainProperties.uploadSessionCleanupCron}")
    public void cleanupExpiredUploadSessions() {
        if (!Boolean.TRUE.equals(fileDomainProperties.getUploadSessionCleanupEnabled())) {
            return;
        }
        fileDomainService.cleanupExpiredUploadSessions();
        log.info("expired upload sessions cleanup finished");
    }
}
