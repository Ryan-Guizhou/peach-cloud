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
 * 过期删除文件清理定时任务
 *
 * <p>定期物理删除超过保留期的逻辑删除文件，释放存储空间。
 * 清理频率通过 {@link FileDomainProperties#getCleanupCron()} 配置。</p>
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
public class FileDeleteCleanupTask {

        private final IFileDomainService fileDomainService;

        private final FileDomainProperties fileDomainProperties;

    /**
     * 执行过期删除文件清理
     *
     * <p>根据配置的 Cron 表达式定期执行，
     * 物理删除所有超过保留期的逻辑删除文件记录及其对应的存储对象。</p>
     */
    @Scheduled(cron = "#{@fileDomainProperties.cleanupCron}")
    public void cleanupExpiredDeletedFiles() {
        if (!Boolean.TRUE.equals(fileDomainProperties.getCleanupEnabled())) {
            return;
        }
        fileDomainService.cleanupExpiredDeletedFiles();
        log.info("expired deleted files cleanup finished");
    }
}
