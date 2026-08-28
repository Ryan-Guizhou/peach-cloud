package com.peach.sample;

import com.peach.email.idempotency.IdempotencyStore;
import com.peach.email.retry.RetryPolicy;
import com.peach.sample.email.idempotency.TTLIdempotencyStore;
import com.peach.sample.email.retry.FixedDelayRetryPolicy;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

/**
 * Sample启动类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/8 15:21
 */
@EnableCaching
@SpringBootApplication
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
        SpringApplication app = new SpringApplication(SampleApplication.class);
        app.setBannerMode(Banner.Mode.CONSOLE);
        app.run(args);
    }
}
