package com.peach.redission.context;

import com.peach.redission.core.ConsumerTask;
import lombok.Data;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 17:35
 * @Description 延迟队列部分
 */
@Data
public class DelayQueuePart {

    private final DelayQueueBasePart delayQueueBasePart;

    private final ConsumerTask consumerTask;

    public DelayQueuePart(DelayQueueBasePart delayQueueBasePart, ConsumerTask consumerTask) {
        this.delayQueueBasePart = delayQueueBasePart;
        this.consumerTask = consumerTask;
    }
}
