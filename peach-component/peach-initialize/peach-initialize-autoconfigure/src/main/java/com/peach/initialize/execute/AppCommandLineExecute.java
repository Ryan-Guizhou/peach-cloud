package com.peach.initialize.execute;

import com.peach.initialize.constant.InitializeHandlerType;
import com.peach.initialize.execute.base.AbstractAppExecute;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 14:56
 */
public class AppCommandLineExecute extends AbstractAppExecute implements ApplicationRunner {


    public AppCommandLineExecute(ConfigurableApplicationContext context) {
        super(context);
    }

    @Override
    public String type() {
        return InitializeHandlerType.APP_COMMAND_LINE_RUNNER;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        executeInit();
    }
}
