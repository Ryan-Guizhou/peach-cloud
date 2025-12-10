package com.peach.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.peach.redis.common.RedisConfig;
import com.peach.redis.common.RedisDaoImpl;

@Slf4j
@Configuration
@ComponentScan(basePackages = "com.peach.redis.common")
public class PeachRedisCommonStarter {

}
