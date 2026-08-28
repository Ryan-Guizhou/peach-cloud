package com.peach.redis.stream;

import org.springframework.data.redis.connection.stream.ObjectRecord;

/**
 * 消息消费者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/18 16:14
 */
@FunctionalInterface
public interface MessageConsumer {

    void accept(ObjectRecord<String, String> message);
}
