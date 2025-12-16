package com.peach.initialize.execute;

import com.peach.initialize.constant.InitializeHandlerType;
import com.peach.initialize.execute.base.AbstractAppExecute;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 14:56
 */
public class AppStartEventExecute extends AbstractAppExecute implements ApplicationListener<ApplicationStartedEvent> {


    public AppStartEventExecute(ConfigurableApplicationContext context) {
        super(context);
    }

    @Override
    public String type() {
        return InitializeHandlerType.APP_EVENT_LISTENER;
    }


    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        executeInit();
    }
}
