package com.peach.initialize.execute;

import com.peach.initialize.constant.InitializeHandlerType;
import com.peach.initialize.execute.base.AbstractAppExecute;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用启动事件执行器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 14:56
 */
public class AppStartEventExecute extends AbstractAppExecute implements ApplicationListener<ApplicationStartedEvent> {

    /** 每个应用上下文仅允许执行一次启动事件初始化，失败也不自动重放部分副作用。 */
    private final AtomicBoolean invoked = new AtomicBoolean();


    public AppStartEventExecute(ConfigurableApplicationContext context) {
        super(context);
    }

    @Override
    public String type() {
        return InitializeHandlerType.APP_EVENT_LISTENER;
    }


    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if (event.getApplicationContext() != context || !invoked.compareAndSet(false, true)) {
            return;
        }
        executeInit();
    }
}
