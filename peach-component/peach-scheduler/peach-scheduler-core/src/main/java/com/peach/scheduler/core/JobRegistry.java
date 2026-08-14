package com.peach.scheduler.core;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.annotation.PeachJob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 线程安全的调度组件。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class JobRegistry {

    /**
     * 创建相关对象。
     */
    public JobRegistry() {
    }

    private final Map<String, JobHandler> handlers = new LinkedHashMap<String, JobHandler>();
    private final Map<String, JobDescriptor> descriptors = new LinkedHashMap<String, JobDescriptor>();

    /**
     * 注册相关能力。
     *
     * @param handler 参数说明
     * @throws IllegalArgumentException 异常说明
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
     * 获取相关数据。
     *
     * @param handlerName 参数说明
     * @return 返回结果
     * @throws IllegalArgumentException 异常说明
     */
    public synchronized JobHandler getRequired(String handlerName) {
        JobHandler handler = handlers.get(handlerName);
        if (handler == null) {
            throw new IllegalArgumentException("Scheduler handler not found: " + handlerName);
        }
        return handler;
    }

    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public synchronized List<JobDescriptor> descriptors() {
        return Collections.unmodifiableList(new ArrayList<JobDescriptor>(descriptors.values()));
    }
}
