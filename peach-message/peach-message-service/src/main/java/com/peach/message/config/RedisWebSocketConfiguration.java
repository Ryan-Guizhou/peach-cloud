package com.peach.message.config;

import com.peach.message.common.MessageConst;
import com.peach.message.websocket.RedisWebSocketSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Indexed;

/**
 * WebSocket Redis配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description WebSocket Redis配置
 */
@Indexed
@Configuration
public class RedisWebSocketConfiguration {

    @Bean(destroyMethod = "destroy")
    public RedisMessageListenerContainer webSocketRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                                RedisWebSocketSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTopicSerializer(new StringRedisSerializer());
        container.addMessageListener(subscriber, new ChannelTopic(MessageConst.WEBSOCKET_REDIS_TOPIC));
        return container;
    }
}
