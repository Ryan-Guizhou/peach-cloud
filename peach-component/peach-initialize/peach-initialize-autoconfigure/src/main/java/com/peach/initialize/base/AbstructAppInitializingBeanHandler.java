package com.peach.initialize.base;

import com.peach.initialize.constant.InitializeHandlerType;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 15:18
 * @Description 应用初始化Bean处理顶级抽象类
 */
public abstract class AbstructAppInitializingBeanHandler implements InitializeHandler{

    @Override
    public String type() {
        return InitializeHandlerType.APP_INITIALIZING_BEAN;
    }
}
