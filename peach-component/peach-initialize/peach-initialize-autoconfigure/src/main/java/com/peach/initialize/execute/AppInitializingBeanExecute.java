package com.peach.initialize.execute;

import com.peach.initialize.constant.InitializeHandlerType;
import com.peach.initialize.execute.base.AbstractAppExecute;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 14:56
 */
public class AppInitializingBeanExecute extends AbstractAppExecute implements InitializingBean {


    public AppInitializingBeanExecute(ConfigurableApplicationContext context) {
        super(context);
    }

    @Override
    public String type() {
        return InitializeHandlerType.APP_INITIALIZING_BEAN;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        executeInit();
    }
}
