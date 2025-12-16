package com.peach.initialize.execute.base;

import com.peach.initialize.base.InitializeHandler;

import org.springframework.context.ConfigurableApplicationContext;

import java.util.Comparator;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 14:55
 * @Description 应用初始化执行顶级抽象类
 */

public abstract class AbstractAppExecute {

    protected final ConfigurableApplicationContext context;

    public AbstractAppExecute(ConfigurableApplicationContext context) {
        this.context = context;
    }

    /**
     * 执行初始化方法
     */
    protected void executeInit(){
        Map<String, InitializeHandler> initializeHandlerMap = context.getBeansOfType(InitializeHandler.class);
        initializeHandlerMap
                .values()
                .stream()
                .filter(initializeHandler -> initializeHandler.type().equals(type()))
                .sorted(Comparator.comparingInt(InitializeHandler::executeOrder))
                .forEach(initializeHandler -> initializeHandler.executeInitialize(context));
    }

    /**
     * 初始化类型
     * @return
     */
    public abstract String type();
}
