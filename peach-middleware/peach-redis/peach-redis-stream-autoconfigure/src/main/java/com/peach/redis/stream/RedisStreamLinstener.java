package com.peach.redis.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;

/**
 * Redis Stream 消费者：group 模式业务处理成功后才 ACK，失败保留 Pending。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Slf4j
public class RedisStreamLinstener implements StreamListener<String, ObjectRecord<String, String>> {

    private final MessageConsumer messageConsumer;
    private final StringRedisTemplate stringRedisTemplate;
    private final String consumerGroup;

    /** 保留广播模式和历史调用方使用的构造入口。 */
    public RedisStreamLinstener(MessageConsumer messageConsumer) {
        this(messageConsumer, null, null);
    }

    /** 消费组模式由容器提供连接模板和组名，用于显式确认消息。 */
    public RedisStreamLinstener(MessageConsumer messageConsumer, StringRedisTemplate stringRedisTemplate,
                               String consumerGroup) {
        this.messageConsumer = java.util.Objects.requireNonNull(messageConsumer, "messageConsumer");
        this.stringRedisTemplate = stringRedisTemplate;
        this.consumerGroup = consumerGroup;
    }

    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        try {
            messageConsumer.accept(message);
            if (consumerGroup != null) {
                if (stringRedisTemplate == null) {
                    throw new IllegalStateException("Redis Stream group template is missing");
                }
                Long acknowledged = stringRedisTemplate.opsForStream()
                        .acknowledge(message.getStream(), consumerGroup, message.getId());
                if (acknowledged == null || acknowledged != 1L) {
                    throw new IllegalStateException("Redis Stream ACK was not confirmed");
                }
            }
            log.debug("Redis Stream message handled, stream={}, id={}", message.getStream(), message.getId());
        } catch (Exception exception) {
            // Do not ACK failed messages; keep PEL state for later claim/recovery.
            log.error("Redis Stream processing failed, stream={}, id={}, group={}",
                    message.getStream(), message.getId(), consumerGroup, exception);
        }
    }
}
