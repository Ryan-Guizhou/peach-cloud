package com.peach.scheduler.core;

/**
 * 任务Descriptor值对象。
 *
 * @param handlerName 业务处理器名称
 * @param description 处理器说明
 * @param jobCode 注解声明的稳定任务编码
 * @param jobName 注解声明的管理端任务名称
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public record JobDescriptor(String handlerName, String description, String jobCode, String jobName) {
    /**
     * 兼容只声明 Handler 能力的已有调用方。
     * @param handlerName Handler 名称
     * @param description 说明
     */
    public JobDescriptor(String handlerName, String description) {
        this(handlerName, description, "", "");
    }
}
