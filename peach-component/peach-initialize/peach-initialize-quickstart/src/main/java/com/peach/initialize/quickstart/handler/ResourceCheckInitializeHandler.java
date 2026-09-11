package com.peach.initialize.quickstart.handler;

import com.peach.initialize.base.AbstractAppStartedEventHandler;
import com.peach.initialize.quickstart.status.InitializeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 资源检查 Handler：与预热同属 {@code APP_EVENT_LISTENER}，用于验证 {@code executeOrder}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:20
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class ResourceCheckInitializeHandler extends AbstractAppStartedEventHandler {

    private final InitializeStatus initializeStatus;

    @Override
    public Integer executeOrder() {
        return 20;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        initializeStatus.record(InitializeStatus.HANDLER_RESOURCE_CHECK);
        log.info("resource check finished, warmedUp={}, beanCount={}",
                initializeStatus.isWarmedUp(), context.getBeanDefinitionCount());
    }
}
