package com.peach.redission.delayqueue.core;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 消费者任务顶级抽象接口
 * 所有延迟队列的消费者都需要实现此接口，定义如何处理延迟消息
 */
public interface ConsumerTask {

    /**
     * 执行消费者任务
     * 当延迟时间到达后，此方法会被调用以处理消息内容
     * 
     * @param content 消息内容，通常是JSON格式的字符串
     * @throws Exception 处理过程中可能抛出的异常
     */
    void execute(String content) throws Exception;

    /**
     * 获取消费者任务主题
     *返回此消费者处理的消息主题名称，用于区分不同类型的消息
     * @return 消息主题名称，不能为空
     */
    String topic();
}