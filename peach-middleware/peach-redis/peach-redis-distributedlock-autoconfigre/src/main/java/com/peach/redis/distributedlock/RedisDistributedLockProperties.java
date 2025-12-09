package com.peach.redis.distributedlock;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "redis.lock")
public class RedisDistributedLockProperties {

    private boolean enabled = true;

    private String keyPrefix = "lock";

    private Duration defaultTtl = Duration.ofSeconds(10);
}

