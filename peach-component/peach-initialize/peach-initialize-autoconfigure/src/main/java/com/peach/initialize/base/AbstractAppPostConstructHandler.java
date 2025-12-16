package com.peach.initialize.base;

import com.peach.initialize.constant.InitializeHandlerType;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 15:22
 * @Description 应用后置构造初始化处理顶级抽象类
 */
public abstract class AbstractAppPostConstructHandler implements InitializeHandler {

    @Override
    public String type() {
        return InitializeHandlerType.APP_POSTCNSTRUCT;
    }

}
