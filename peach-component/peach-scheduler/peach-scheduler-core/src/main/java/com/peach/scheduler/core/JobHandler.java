package com.peach.scheduler.core;

/**
 * 业务调度相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface JobHandler {

    /**
     * 调度模块相关说明。
     *
     * <p>调度模块相关说明。</p>
     *
     * @param context 参数说明
     * @return 返回结果
     * @throws Exception 异常说明
     */
    JobResult execute(JobContext context) throws Exception;
}
