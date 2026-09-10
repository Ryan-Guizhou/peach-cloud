package com.peach.scheduler.provider;

import com.peach.scheduler.model.JobDefinition;
import com.peach.scheduler.model.SchedulerCapability;
import java.util.Set;

/**
 * 调度 provider 抽象，负责把 Peach 调度定义映射到具体调度引擎。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface SchedulingProvider {

    /**
     * 获取 provider 标识。
     *
     * @return provider 标识。
     */
    String getProviderId();

    /**
     * 获取 provider 支持的调度能力。
     *
     * @return 调度能力集合。
     */
    Set<SchedulerCapability> getCapabilities();

    /**
     * 新建调度定义。
     *
     * @param definition 调度定义。
     */
    void schedule(JobDefinition definition);

    /**
     * 更新已存在的调度定义。
     *
     * @param definition 调度定义。
     */
    void reschedule(JobDefinition definition);

    /**
     * 暂停指定任务。
     *
     * @param jobCode 任务编码。
     */
    void pause(String jobCode);

    /**
     * 恢复指定任务。
     *
     * @param jobCode 任务编码。
     */
    void resume(String jobCode);

    /**
     * 删除指定任务。
     *
     * @param jobCode 任务编码。
     */
    void delete(String jobCode);

    /**
     * 立即触发指定任务。
     *
     * @param jobCode 任务编码。
     * @param parameters 本次触发传入的任务参数。
     */
    void trigger(String jobCode, String parameters);

    /**
     * 判断任务是否已注册到当前 provider。
     *
     * @param jobCode 任务编码。
     * @return 已存在返回 {@code true}。
     */
    boolean exists(String jobCode);
}
