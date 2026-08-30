package com.peach.scheduler.core;

/**
 * 任务Descriptor值对象。
 *
 * @param handlerName 业务处理器名称
 * @param description 处理器说明
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public record JobDescriptor(String handlerName, String description) {
}
