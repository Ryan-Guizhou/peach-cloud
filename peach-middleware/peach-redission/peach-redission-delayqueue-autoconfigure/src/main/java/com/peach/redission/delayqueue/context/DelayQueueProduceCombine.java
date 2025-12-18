package com.peach.redission.delayqueue.context;

import com.peach.redission.delayqueue.core.DelayProduceQueue;
import com.peach.redission.delayqueue.core.IsolationRegionSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 18:01
 * @Description 延迟队列发送者 分片选择
 */
public class DelayQueueProduceCombine {
    
    private final IsolationRegionSelector isolationRegionSelector;
    
    private final List<DelayProduceQueue> delayProduceQueueList = new ArrayList<>();
    
    public DelayQueueProduceCombine(DelayQueueBasePart delayQueueBasePart,String topic){
        Integer isolationRegionCount = delayQueueBasePart.getDelayQueueProperties().getIsolationRegionCount();
        isolationRegionSelector =new IsolationRegionSelector(isolationRegionCount);
        //开始分片
        for(int i = 0; i < isolationRegionCount; i++) {
            delayProduceQueueList.add(new DelayProduceQueue(delayQueueBasePart.getRedissonClient(),topic + "-" + i));
        }
    }
    
    public void offer(String content,long delayTime, TimeUnit timeUnit){
        int index = isolationRegionSelector.getIndex();
        //拿取分片
        delayProduceQueueList.get(index).offer(content, delayTime, timeUnit);
    }
}