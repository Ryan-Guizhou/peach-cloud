package com.peach.redis.stream.config;

import com.peach.redis.stream.constant.RedisStreamContant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/18 16:12
 */
@Data
@ConfigurationProperties(prefix = RedisStreamContant.REDIS_STREAM_PREFIX)
public class RedisStreamProperties {

    /**
     * stream名字
     * */
    private String streamName = "user-log";

    /**
     * 消费组名字
     * */
    private String consumerGroup = "user-log-group";

    /**
     * 消费者名
     * */
    private String consumerName = "user-log-consumer";

    /**
     * 消费方式 group:消费组(默认)/broadcast:广播
     */
    private String consumerType = RedisStreamContant.GROUP;
}
