package com.peach.scheduler.provider;

import com.peach.scheduler.model.JobDefinition;
import com.peach.scheduler.model.SchedulerCapability;
import java.util.Set;

/**
 * 调度引擎相关说明。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface SchedulingProvider {

    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    String getProviderId();

    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    Set<SchedulerCapability> getCapabilities();

    /**
     * 创建相关对象。
     *
     * @param definition 参数说明
     */
    void schedule(JobDefinition definition);

    /**
     * 调度模块相关说明。
     *
     * @param definition 参数说明
     */
    void reschedule(JobDefinition definition);

    /**
     * 调度模块相关说明。
     *
     * @param jobCode 参数说明
     */
    void pause(String jobCode);

    /**
     * 调度模块相关说明。
     *
     * @param jobCode 参数说明
     */
    void resume(String jobCode);

    /**
     * 调度模块相关说明。
     *
     * @param jobCode 参数说明
     */
    void delete(String jobCode);

    /**
     * 调度模块相关说明。
     *
     * @param jobCode 参数说明
     * @param parameters 参数说明
     */
    void trigger(String jobCode, String parameters);

    /**
     * 调度模块相关说明。
     *
     * @param jobCode 参数说明
     * @return 返回结果
     */
    boolean exists(String jobCode);
}
