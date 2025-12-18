package com.peach.redission.delayqueue.event;


import com.peach.common.util.PeachCollectionUtil;
import com.peach.initialize.base.AbstractAppStartedEventHandler;
import com.peach.redission.delayqueue.context.DelayQueueBasePart;
import com.peach.redission.delayqueue.context.DelayQueuePart;
import com.peach.redission.delayqueue.core.ConsumerTask;
import com.peach.redission.delayqueue.core.DelayConsumerQueue;
import com.peach.redission.delayqueue.core.ReliableDelayConsumerQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 延迟队列初始化处理
 */
@Slf4j
public class DelayQueueInitHandler extends AbstractAppStartedEventHandler {

    private final DelayQueueBasePart delayQueueBasePart;

    public DelayQueueInitHandler(DelayQueueBasePart delayQueueBasePart) {
        this.delayQueueBasePart = delayQueueBasePart;
        log.debug("Initialized DelayQueueInitHandler");
    }


    @Override
    public Integer executeOrder() {
        return 0;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        Map<String, ConsumerTask> consumerTaskMap = context.getBeansOfType(ConsumerTask.class);
        if (PeachCollectionUtil.isEmpty(consumerTaskMap)){
            log.info("No ConsumerTask beans found, skipping delay queue initialization");
            return;
        }
        
        log.info("Found {} ConsumerTask beans, initializing delay queues", consumerTaskMap.size());
        
        // 检查是否使用可靠队列
        boolean useReliableQueue = delayQueueBasePart.getDelayQueueProperties().getUseReliableQueue();
        log.info("Using reliable queue: {}", useReliableQueue);
        
        for (ConsumerTask consumerTask : consumerTaskMap.values()) {
            DelayQueuePart delayQueuePart = new DelayQueuePart(delayQueueBasePart, consumerTask);
            Integer isolationRegionCount = delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties()
                    .getIsolationRegionCount();
                    
            log.info("Initializing delay queue for topic: {} with {} isolation regions", 
                    consumerTask.topic(), isolationRegionCount);
                    
            for (int i = 0; i < isolationRegionCount; i++) {
                String topic = consumerTask.topic() + "-" + i;
                if (useReliableQueue) {
                    // 使用可靠的延迟消费者队列
                    ReliableDelayConsumerQueue delayConsumerQueue = new ReliableDelayConsumerQueue(delayQueuePart, topic);
                    delayConsumerQueue.startListener();
                    log.debug("Started reliable listener for topic: {}", topic);
                } else {
                    // 使用原有的延迟消费者队列
                    DelayConsumerQueue delayConsumerQueue = new DelayConsumerQueue(delayQueuePart, topic);
                    delayConsumerQueue.startListener();
                    log.debug("Started standard listener for topic: {}", topic);
                }
            }
        }
        
        log.info("Delay queue initialization completed");
    }
}