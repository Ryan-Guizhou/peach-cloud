package com.peach.threadpool;

import com.peach.threadpool.config.ThreadPoolProperties;
import com.peach.threadpool.core.ThreadPoolAspect;
import com.peach.threadpool.manager.ThreadPoolManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 线程线程池Auto自动配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 17:52
 */
@AutoConfiguration
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(ThreadPoolProperties.class)
    public ThreadPoolManager threadPoolManager(ThreadPoolProperties threadPoolProperties) {
        return new ThreadPoolManager(threadPoolProperties);
    }

    @Bean
    @ConditionalOnBean(ThreadPoolManager.class)
    @ConditionalOnMissingBean(ThreadPoolAspect.class)
    public ThreadPoolAspect threadPoolAspect(ThreadPoolManager threadPoolManager) {
        return new ThreadPoolAspect(threadPoolManager);
    }
}
