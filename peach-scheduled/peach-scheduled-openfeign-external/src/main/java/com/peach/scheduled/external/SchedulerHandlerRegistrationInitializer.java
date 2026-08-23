package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.scheduler.config.PeachSchedulerProperties;
import com.peach.scheduler.core.JobDescriptor;
import com.peach.scheduler.core.JobRegistry;
import com.peach.scheduled.dto.HandlerRegistrationDTO;
import java.net.InetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 注册相关能力。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerHandlerRegistrationInitializer implements SmartInitializingSingleton {
    private static final Logger log = LoggerFactory.getLogger(SchedulerHandlerRegistrationInitializer.class);
    private final JobRegistry registry;
    private final SchedulerHandlerExternalClient client;
    private final PeachSchedulerProperties properties;

    /**
     * 创建相关对象。
     * @param registry 本地 Job Handler 注册表
     * @param client Scheduler 控制面 Feign Client
     * @param properties Scheduler 执行器配置
     */
    public SchedulerHandlerRegistrationInitializer(JobRegistry registry,
                                                   SchedulerHandlerExternalClient client,
                                                   PeachSchedulerProperties properties) {
        this.registry = registry;
        this.client = client;
        this.properties = properties;
    }

    /**
     * 注册相关能力。
     */
    @Override
    public void afterSingletonsInstantiated() {
        register();
    }

    /**
     * 调度模块相关说明。
     */
    @Scheduled(fixedDelayString = "${peach.scheduler.executor.handler-heartbeat-ms:60000}")
    public void heartbeat() {
        register();
    }

    private void register() {
        HandlerRegistrationDTO request = new HandlerRegistrationDTO();
        request.setApplicationName(properties.getExecutor().getApplicationName());
        request.setInstanceId(resolveInstanceId());
        for (JobDescriptor descriptor : registry.descriptors()) {
            HandlerRegistrationDTO.Item item = new HandlerRegistrationDTO.Item();
            item.setHandlerName(descriptor.getHandlerName());
            item.setDescription(descriptor.getDescription());
            request.getHandlers().add(item);
        }
        if (request.getHandlers().isEmpty()) {
            log.info("Scheduler handler registration skipped because no handlers are registered, applicationName={}",
                    request.getApplicationName());
            return;
        }
        if (request.getApplicationName() == null || request.getApplicationName().isBlank()) {
            log.warn("Scheduler handler registration skipped because applicationName is blank");
            return;
        }
        try {
            Response response = client.register(request);
            if (response == null || !response.isSuccess()) {
                log.warn("Scheduler handler registration was not acknowledged, applicationName={}, handlerCount={}",
                        request.getApplicationName(), request.getHandlers().size());
            }
        } catch (RuntimeException ex) {
            log.warn("Scheduler handler registration failed, applicationName={}, handlerCount={}, errorType={}",
                    request.getApplicationName(), request.getHandlers().size(), ex.getClass().getName());
        }
    }

    private String resolveInstanceId() {
        String configured = properties.getExecutor().getInstanceId();
        if (configured != null && !configured.isBlank()) return configured;
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception ex) {
            return "unknown-instance";
        }
    }
}
