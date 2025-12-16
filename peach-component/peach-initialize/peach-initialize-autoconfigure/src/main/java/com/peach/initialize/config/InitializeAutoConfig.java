package com.peach.initialize.config;

import com.peach.initialize.execute.AppCommandLineExecute;
import com.peach.initialize.execute.AppInitializingBeanExecute;
import com.peach.initialize.execute.AppPostConstructExecute;
import com.peach.initialize.execute.AppStartEventExecute;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 15:00
 */
@AutoConfiguration
public class InitializeAutoConfig {

    @Bean
    public AppInitializingBeanExecute appInitializingBeanExecute(ConfigurableApplicationContext context) {
        return new AppInitializingBeanExecute(context);
    }

    @Bean
    public AppPostConstructExecute appPostConstructExecute(ConfigurableApplicationContext context) {
        return new AppPostConstructExecute(context);
    }

    @Bean
    public AppCommandLineExecute appCommandLineExecute(ConfigurableApplicationContext context) {
        return new AppCommandLineExecute(context);
    }

    @Bean
    public AppStartEventExecute appStartEventExecute(ConfigurableApplicationContext context) {
        return new AppStartEventExecute(context);
    }
}
