package com.peach.redission.core;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 17:25
 * @Description 消费者任务 顶级抽象接口
 */
public interface ConsumerTask {

    /**
     * 消费者任务
     * @param content
     */
    void execute(String content);

    /**
     * 获取消费者任务主题
     * @return
     */
    String topic();
}
