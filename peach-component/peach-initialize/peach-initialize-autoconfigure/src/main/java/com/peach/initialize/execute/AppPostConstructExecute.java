package com.peach.initialize.execute;


import com.peach.initialize.constant.InitializeHandlerType;
import com.peach.initialize.execute.base.AbstractAppExecute;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 15:14
 */
public class AppPostConstructExecute extends AbstractAppExecute {

    public AppPostConstructExecute(ConfigurableApplicationContext context) {
        super(context);
    }

    @PostConstruct
    public void postConstructExecute() {
        executeInit();
    }

    @Override
    public String type() {
        return InitializeHandlerType.APP_POSTCNSTRUCT;
    }


}
