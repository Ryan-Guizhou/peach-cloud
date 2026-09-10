package com.peach.scheduler.quartz;

import org.quartz.DisallowConcurrentExecution;

/**
 * 禁止同一任务并发触发的 Quartz Job。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@DisallowConcurrentExecution
public class PeachQuartzDisallowConcurrentJob extends AbstractPeachQuartzJob {

    /**
     * 创建禁止并发触发的 Quartz Job。
     */
    public PeachQuartzDisallowConcurrentJob() {
        super();
    }
}
