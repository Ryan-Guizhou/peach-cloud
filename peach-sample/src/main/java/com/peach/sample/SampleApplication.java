package com.peach.sample;

import com.peach.email.Idempotency.IdempotencyStore;
import com.peach.email.Idempotency.SimpleIdempotencyStore;
import com.peach.email.retry.RetryPolicy;
import com.peach.redis.common.RedisConfig;
import com.peach.sample.email.Idempotency.TTLIdempotencyStore;
import com.peach.sample.email.retry.FixedDelayRetryPolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/8 15:21
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.peach.redis", "com.peach.sample"})
public class SampleApplication {

    /**
     * 重写固定间隔重试策略替换starter中的默认策略
     * @return
     */
    @Bean
    public RetryPolicy retryPolicy() {
        return new FixedDelayRetryPolicy();
    }

    /**
     * 重写IdempotencyStore替换starter中的默认实现
     * @return
     */
    @Bean
    public IdempotencyStore idempotencyStore() {
        return new TTLIdempotencyStore();
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
