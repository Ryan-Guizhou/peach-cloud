package com.peach.redission.context;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 18:01
 * @Description 延迟队列上下文
 */
public class DelayQueueContext {

    private final DelayQueueBasePart delayQueueBasePart;

    private final Map<String, DelayQueueProduceCombine> delayQueueProduceCombineMap = new ConcurrentHashMap<>();

    public DelayQueueContext(DelayQueueBasePart delayQueueBasePart) {
        this.delayQueueBasePart = delayQueueBasePart;
    }

    public void sendMessage(String topic, String content, long delayTime, TimeUnit timeUnit){
        DelayQueueProduceCombine delayQueueProduceCombine = delayQueueProduceCombineMap.computeIfAbsent(
                topic, k -> new DelayQueueProduceCombine(delayQueueBasePart,topic));
        delayQueueProduceCombine.offer(content,delayTime,timeUnit);
    }
}

