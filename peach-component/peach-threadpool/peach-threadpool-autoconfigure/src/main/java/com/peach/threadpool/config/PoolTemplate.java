package com.peach.threadpool.config;

import com.peach.threadpool.constant.ThreadConstant;
import com.peach.threadpool.core.PoolType;
import com.peach.threadpool.core.RejectedPolicy;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/6 10:54
 */
public class PoolTemplate {

    private PoolTemplate(){
        throw new IllegalStateException("Utility class");
    }

    /**
     * CPU密集型默认配置
     * @return
     */
    public static PoolProperties cpuTemplate() {
        PoolProperties props = new PoolProperties();
        props.setType(PoolType.CPU);
        props.setCoreSize(Runtime.getRuntime().availableProcessors() + 1);
        props.setMaxSize(Runtime.getRuntime().availableProcessors() * 2);
        props.setQueueCapacity(1024);
        props.setKeepAliveSeconds(60);
        props.setAllowCoreThreadTimeOut(false);
        props.setPrestartCoreThreads(true);
        props.setThreadNamePrefix(ThreadConstant.THREAD_POOL_NAME_PREFIX);
        props.setRejectedPolicy(RejectedPolicy.CALLER_RUNS);
        return props;
    }

    /**
     * IO密集型默认配置
     * @return
     */
    public static PoolProperties ioTemplate() {
        PoolProperties props = new PoolProperties();
        props.setType(PoolType.IO);
        props.setCoreSize(Runtime.getRuntime().availableProcessors() * 2);
        props.setMaxSize(Runtime.getRuntime().availableProcessors() * 4);
        props.setQueueCapacity(2048);
        props.setKeepAliveSeconds(30);
        props.setAllowCoreThreadTimeOut(true);
        props.setPrestartCoreThreads(false);
        props.setThreadNamePrefix(ThreadConstant.THREAD_POOL_NAME_PREFIX);
        props.setRejectedPolicy(RejectedPolicy.ABORT);
        return props;
    }
}
