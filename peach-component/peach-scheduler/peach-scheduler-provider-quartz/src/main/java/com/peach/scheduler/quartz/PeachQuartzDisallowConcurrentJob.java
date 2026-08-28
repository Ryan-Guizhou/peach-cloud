package com.peach.scheduler.quartz;

import org.quartz.DisallowConcurrentExecution;

/**
 * PeachQuartzDisallowConcurrentJob相关类。
 * <p>调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@DisallowConcurrentExecution
public class PeachQuartzDisallowConcurrentJob extends AbstractPeachQuartzJob {

    /**
     * 创建实例。
     */
    public PeachQuartzDisallowConcurrentJob() {
        super();
    }
}
