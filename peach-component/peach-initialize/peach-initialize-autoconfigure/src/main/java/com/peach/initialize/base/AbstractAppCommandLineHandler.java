package com.peach.initialize.base;

import com.peach.initialize.constant.InitializeHandlerType;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 14:52
 * @Description 应用命令行初始化处理顶级抽象类
 */
public abstract class AbstractAppCommandLineHandler implements InitializeHandler {

    @Override
    public String type() {
        return InitializeHandlerType.APP_COMMAND_LINE_RUNNER;
    }
}
