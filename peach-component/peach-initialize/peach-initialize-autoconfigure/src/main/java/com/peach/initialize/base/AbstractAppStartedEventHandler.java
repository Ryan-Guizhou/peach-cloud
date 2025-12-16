package com.peach.initialize.base;

import com.peach.initialize.constant.InitializeHandlerType;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 15:24
 * @Description 抽象的启动事件处理器顶级抽象类
 */
public abstract class AbstractAppStartedEventHandler implements InitializeHandler{

    @Override
    public String type() {
        return InitializeHandlerType.APP_EVENT_LISTENER;
    }
}
