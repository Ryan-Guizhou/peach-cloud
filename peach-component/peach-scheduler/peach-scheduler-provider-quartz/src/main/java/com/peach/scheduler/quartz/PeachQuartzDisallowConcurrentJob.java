package com.peach.scheduler.quartz;

import org.quartz.DisallowConcurrentExecution;

/**
 * 调度模块相关说明。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@DisallowConcurrentExecution
public class PeachQuartzDisallowConcurrentJob extends AbstractPeachQuartzJob {

    /**
     * 创建相关对象。
     */
    public PeachQuartzDisallowConcurrentJob() {
        super();
    }
}
