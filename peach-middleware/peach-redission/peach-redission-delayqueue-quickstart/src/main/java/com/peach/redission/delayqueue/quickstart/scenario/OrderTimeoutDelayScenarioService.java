package com.peach.redission.delayqueue.quickstart.scenario;

import com.peach.redission.delayqueue.context.DelayQueueContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 订单超时延迟队列场景：演示 DelayQueueContext.sendMessage。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class OrderTimeoutDelayScenarioService {

    private final DelayQueueContext delayQueueContext;

    /**
     * @param delayQueueContext 延迟队列上下文
     */
    public OrderTimeoutDelayScenarioService(DelayQueueContext delayQueueContext) {
        this.delayQueueContext = delayQueueContext;
    }

    /**
     * 投递短延迟订单超时消息。
     *
     * @param orderId     订单标识
     * @param delaySeconds 延迟秒数
     */
    public void sendTimeoutMessage(String orderId, long delaySeconds) {
        delayQueueContext.sendMessage(
                OrderTimeoutConsumerTask.TOPIC,
                "orderId=" + orderId,
                delaySeconds,
                TimeUnit.SECONDS);
    }
}
