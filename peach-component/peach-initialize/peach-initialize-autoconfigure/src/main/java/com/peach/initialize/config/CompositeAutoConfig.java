package com.peach.initialize.config;

import com.peach.initialize.impl.composite.CompositeContainer;
import com.peach.initialize.impl.composite.init.CompositeInit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 16:28
 */
@AutoConfiguration
public class CompositeAutoConfig {

    @Bean
    public CompositeContainer compositeContainer() {
        return new CompositeContainer();
    }

    @Bean
    public CompositeInit compositeInit(CompositeContainer compositeContainer) {
        return new CompositeInit(compositeContainer);
    }
}
