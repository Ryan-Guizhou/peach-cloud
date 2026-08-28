package com.peach.threadpool.config;

import com.peach.threadpool.constant.ThreadConstant;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 线程线程池配置属性。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 17:51
 */
@ConfigurationProperties(ThreadConstant.THREAD_POOL_CONFIG_PREFIX)
public class ThreadPoolProperties {
    
    private GlobalProperties global = new GlobalProperties();

    private List<PoolProperties> pools = new ArrayList<>();

    public  GlobalProperties getGlobal() {
        return global;
    }

    public void setGlobal(GlobalProperties global) {
        this.global = global;
    }

    public List<PoolProperties> getPools() {
        return pools;
    }

    public void setPools(List<PoolProperties> pools) {
        this.pools = pools;
    }

}
