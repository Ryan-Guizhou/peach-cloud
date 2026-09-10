package com.peach.scheduler.quartz;

/**
 * 允许并发触发的 Quartz Job。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class PeachQuartzConcurrentJob extends AbstractPeachQuartzJob {

    /**
     * 创建允许并发触发的 Quartz Job。
     */
    public PeachQuartzConcurrentJob() {
        super();
    }
}
