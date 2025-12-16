package com.peach.sample.initialize;

import com.peach.initialize.base.AbstractAppPostConstructHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 15:33
 */
@Slf4j
@Component
public class CustomPostConstruct extends AbstractAppPostConstructHandler {
    @Override
    public Integer executeOrder() {
        return 1;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        log.info("CustomPostConstruct executeInitialize ");
    }
}
