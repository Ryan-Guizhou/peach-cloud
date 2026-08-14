package com.peach.threadpool.manager;


import com.peach.threadpool.config.GlobalProperties;
import com.peach.threadpool.config.PoolProperties;
import com.peach.threadpool.config.PoolTemplate;
import com.peach.threadpool.config.ThreadPoolProperties;
import com.peach.threadpool.core.NamedThreadFactory;
import com.peach.threadpool.core.PoolType;
import com.peach.threadpool.core.TaskWrapper;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/5 17:54
 */
public class ThreadPoolManager {

    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();

    private final TaskWrapper wrapper;

    public ThreadPoolManager(ThreadPoolProperties properties) {
        GlobalProperties global = properties.getGlobal();
        this.wrapper = new TaskWrapper(global.isEnableSecurityContext());
        List<PoolProperties> list = properties.getPools();
        if (CollectionUtils.isEmpty(list)) {
            list = new ArrayList<>();
            list.add(PoolTemplate.cpuTemplate());
            list.add(PoolTemplate.ioTemplate());
        }
        for (PoolProperties p : list) {
            ExecutorService pool = createPool(p);
            executors.put(p.getType().name().toLowerCase(Locale.ROOT), pool);
        }
    }

    public ExecutorService get(PoolType poolType) {
        String keyName = poolType.name().toLowerCase(Locale.ROOT);
        return executors.computeIfAbsent(keyName,k-> createDefaultPool());
    }

    /**
     * 创建IO类型的默认线程池
     * @return
     */
    private ExecutorService createDefaultPool() {
        PoolProperties p = new PoolProperties();
        p.setCoreSize(Runtime.getRuntime().availableProcessors() * 2);
        p.setMaxSize(p.getCoreSize() * 2);
        p.setAllowCoreThreadTimeOut(true);
        p.setKeepAliveSeconds(60);
        p.setQueueCapacity(100);
        return createPool(p);
    }

    public <T> Future<T> submit(PoolType poolType, Callable<T> task) {
        return get(poolType).submit(wrapper.wrap(task));
    }

    public Future<?> submit(PoolType poolType, Runnable task) {
        return get(poolType).submit(wrapper.wrap(task));
    }

    public void execute(PoolType poolType, Runnable task) {
        get(poolType).execute(wrapper.wrap(task));
    }

    /**
     * 创建线程池
     * @param p
     * @return
     */
    private ExecutorService createPool(PoolProperties p) {
        BlockingQueue<Runnable> queue = p.getQueueCapacity() <= 0
                ? new SynchronousQueue<>()
                : new LinkedBlockingQueue<>(p.getQueueCapacity());
        int max = p.getMaxSize();
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(
                p.getCoreSize(),
                max,
                p.getKeepAliveSeconds(), TimeUnit.SECONDS,
                queue,
                new NamedThreadFactory(p.getThreadNamePrefix(),p.getType(), false),
                p.getRejectedPolicy().handler()
        );
        tpe.allowCoreThreadTimeOut(p.isAllowCoreThreadTimeOut());
        if (p.isPrestartCoreThreads()) {
            tpe.prestartAllCoreThreads();
        }
        return tpe;

    }

}
