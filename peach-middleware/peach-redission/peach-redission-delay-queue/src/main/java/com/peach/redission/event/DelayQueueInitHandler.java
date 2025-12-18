package com.peach.redission.event;


import com.peach.common.util.PeachCollectionUtil;
import com.peach.initialize.base.AbstractAppStartedEventHandler;
import com.peach.redission.context.DelayQueueBasePart;
import com.peach.redission.context.DelayQueuePart;
import com.peach.redission.core.ConsumerTask;
import com.peach.redission.core.DelayConsumerQueue;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 延迟队列初始化处理
 */
public class DelayQueueInitHandler extends AbstractAppStartedEventHandler {

    private final DelayQueueBasePart delayQueueBasePart;

    public DelayQueueInitHandler(DelayQueueBasePart delayQueueBasePart) {
        this.delayQueueBasePart = delayQueueBasePart;
    }


    @Override
    public Integer executeOrder() {
        return 0;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        Map<String, ConsumerTask> consumerTaskMap = context.getBeansOfType(ConsumerTask.class);
        if (PeachCollectionUtil.isEmpty(consumerTaskMap)){
            return;
        }
        for (ConsumerTask consumerTask : consumerTaskMap.values()) {
            DelayQueuePart delayQueuePart = new DelayQueuePart(delayQueueBasePart, consumerTask);
            Integer isolationRegionCount = delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties()
                    .getIsolationRegionCount();
            for (int i = 0; i < isolationRegionCount; i++) {
                DelayConsumerQueue delayConsumerQueue = new DelayConsumerQueue(delayQueuePart, consumerTask.topic() + "-" + i);
                delayConsumerQueue.startListener();
            }
        }
    }
}
