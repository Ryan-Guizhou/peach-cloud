package com.peach.redission.delayqueue.event;

import com.peach.initialize.base.AbstractAppStartedEventHandler;
import com.peach.redission.delayqueue.config.DelayQueueProperties;
import com.peach.redission.delayqueue.core.ConsumerTask;
import com.peach.redission.delayqueue.core.RedisDelayQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 统一可靠队列消费启动器；消费者仍实现原有 ConsumerTask 契约。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Slf4j
public class DelayQueueInitHandler extends AbstractAppStartedEventHandler {

    private final RedisDelayQueue queue;
    private final DelayQueueProperties properties;

    public DelayQueueInitHandler(RedisDelayQueue queue, DelayQueueProperties properties) {
        this.queue = queue;
        this.properties = properties;
    }

    @Override
    public Integer executeOrder() {
        return 0;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        Map<String, ConsumerTask> tasks = context.getBeansOfType(ConsumerTask.class);
        if (tasks.isEmpty()) {
            log.debug("No delay queue consumers registered");
            return;
        }
        Set<String> topics = new HashSet<>();
        // 先校验所有 topic 与所有历史分区，失败时不能启动任何一个新消费者。
        for (ConsumerTask task : tasks.values()) {
            String topic = task.topic();
            if (topic == null || topic.isBlank() || !topics.add(topic)) {
                throw new IllegalStateException("Duplicate or invalid delay queue consumer topic: " + topic);
            }
        }
        for (String topic : topics) {
            for (int partition = 0; partition < properties.getIsolationRegionCount(); partition++) {
                queue.assertLegacyDrained(topic + "-" + partition);
            }
        }
        for (ConsumerTask task : tasks.values()) {
            String topic = task.topic();
            for (int partition = 0; partition < properties.getIsolationRegionCount(); partition++) {
                String partitionTopic = topic + "-" + partition;
                ConsumerTask consumer = new ConsumerTask() {
                    @Override
                    public void execute(String content) {
                        task.execute(content);
                    }

                    @Override
                    public String topic() {
                        return partitionTopic;
                    }
                };
                queue.start(partitionTopic, consumer);
            }
            log.info("Delay queue consumer started, topic={}, partitions={}",
                    topic, properties.getIsolationRegionCount());
        }
    }
}
