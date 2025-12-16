package com.peach.initialize.impl.composite.init;

import com.peach.initialize.base.AbstractAppStartedEventHandler;
import com.peach.initialize.impl.composite.CompositeContainer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 16:26
 */
public class CompositeInit extends AbstractAppStartedEventHandler {

    private final CompositeContainer compositeContainer;

    public CompositeInit(CompositeContainer compositeContainer) {
        this.compositeContainer = compositeContainer;
    }

    @Override
    public Integer executeOrder() {
        return 1;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        compositeContainer.init(context);
    }
}
