package com.peach.scheduler.provider;

import com.peach.scheduler.model.JobDefinition;
import com.peach.scheduler.model.SchedulerCapability;
import java.util.Set;

/**
 * Scheduling提供者。
 * <p>调度模块说明。
 * 调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface SchedulingProvider {

    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    String getProviderId();

    /**
     * 获取相关数据。
     *
     * @return 执行结果。
     */
    Set<SchedulerCapability> getCapabilities();

    /**
     * 创建实例。
     *
     * @param definition definition。
     */
    void schedule(JobDefinition definition);

    /**
     * 调度模块说明。
     *
     * @param definition definition。
     */
    void reschedule(JobDefinition definition);

    /**
     * 调度模块说明。
     *
     * @param jobCode job Code。
     */
    void pause(String jobCode);

    /**
     * 调度模块说明。
     *
     * @param jobCode job Code。
     */
    void resume(String jobCode);

    /**
     * 调度模块说明。
     *
     * @param jobCode job Code。
     */
    void delete(String jobCode);

    /**
     * 调度模块说明。
     *
     * @param jobCode job Code。
     * @param parameters parameters。
     */
    void trigger(String jobCode, String parameters);

    /**
     * 调度模块说明。
     *
     * @param jobCode job Code。
     * @return 执行结果。
     */
    boolean exists(String jobCode);
}
