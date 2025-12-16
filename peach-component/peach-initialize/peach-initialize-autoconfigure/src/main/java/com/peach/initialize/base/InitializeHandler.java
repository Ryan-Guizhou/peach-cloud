package com.peach.initialize.base;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/16 14:50
 * @Description 初始化处理顶级抽象接口
 */
public interface InitializeHandler {

    /**
     * 初始化类型
     * @return 类型
     */
    String type();

    /**
     * 执行顺序
     * @return 顺序
     */
    Integer executeOrder();

    /**
     * 执行初始化
     */
    void executeInitialize(ConfigurableApplicationContext context);
}
