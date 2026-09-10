package com.peach.scheduler.core;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.annotation.PeachJob;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 {@link PeachJob} 注解维护任务处理器注册表。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class JobRegistry {

    private final Map<String, JobHandler> handlers = new LinkedHashMap<>();
    private final Map<String, JobDescriptor> descriptors = new LinkedHashMap<>();

    /**
     * 注册任务处理器。
     *
     * @param handler 带有 {@link PeachJob} 注解的任务处理器。
     * @throws IllegalArgumentException 处理器未声明名称或名称重复时抛出。
     */
    public synchronized void register(JobHandler handler) {
        PeachJob annotation = handler.getClass().getAnnotation(PeachJob.class);
        if (annotation == null) {
            throw new IllegalArgumentException("JobHandler must declare @PeachJob: " + handler.getClass().getName());
        }
        String name = annotation.value() == null ? "" : annotation.value().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("@PeachJob value must not be blank: " + handler.getClass().getName());
        }
        if (handlers.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate scheduler handler name: " + name);
        }
        handlers.put(name, handler);
        descriptors.put(name, new JobDescriptor(name, annotation.description()));
    }

    /**
     * 获取指定名称的任务处理器。
     *
     * @param handlerName 任务处理器名称。
     * @return 任务处理器。
     * @throws IllegalArgumentException 处理器不存在时抛出。
     */
    public synchronized JobHandler getRequired(String handlerName) {
        JobHandler handler = handlers.get(handlerName);
        if (handler == null) {
            throw new IllegalArgumentException("Scheduler handler not found: " + handlerName);
        }
        return handler;
    }

    /**
     * 获取已注册任务处理器描述。
     *
     * @return 任务处理器描述列表。
     */
    public synchronized List<JobDescriptor> descriptors() {
        return List.copyOf(descriptors.values());
    }
}
