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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 受管线程池入口。
 *
 * <p>负责创建不同任务类型的线程池，并在提交任务时传播日志、链路和安全上下文。调用方不应
 * 绕过该入口创建无法统一关闭和观测的游离线程池。</p>
 */
public class ThreadPoolManager {

    private final Map<String, ExecutorService> executors = new ConcurrentHashMap<>();

    private final TaskWrapper wrapper;

    public ThreadPoolManager(ThreadPoolProperties properties) {
        GlobalProperties global = properties.getGlobal();
        this.wrapper = new TaskWrapper(global.isEnableMdc(), global.isEnableSecurityContext());
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

    /**
     * 获取指定类型的受管线程池。
     *
     * @param poolType 线程池类型
     * @return 可复用的线程池实例
     */
    public ExecutorService get(PoolType poolType) {
        String keyName = poolType.name().toLowerCase(Locale.ROOT);
        return executors.computeIfAbsent(keyName,k-> createDefaultPool());
    }

    /**
     * 创建 IO 类型的默认线程池。
     *
     * @return 默认线程池
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

    /**
     * 提交有返回值的任务。
     *
     * @param poolType 线程池类型
     * @param task 待执行任务
     * @param <T> 返回值类型
     * @return 任务 Future
     */
    public <T> Future<T> submit(PoolType poolType, Callable<T> task) {
        return get(poolType).submit(wrapper.wrap(task));
    }

    /**
     * 提交无返回值任务。
     *
     * @param poolType 线程池类型
     * @param task 待执行任务
     * @return 任务 Future
     */
    public Future<?> submit(PoolType poolType, Runnable task) {
        return get(poolType).submit(wrapper.wrap(task));
    }

    /**
     * 执行无返回值任务。
     *
     * @param poolType 线程池类型
     * @param task 待执行任务
     */
    public void execute(PoolType poolType, Runnable task) {
        get(poolType).execute(wrapper.wrap(task));
    }

    /**
     * 根据配置创建线程池。
     *
     * @param p 线程池配置
     * @return 新线程池
     */
    private ExecutorService createPool(PoolProperties p) {
        if (p.getType() == PoolType.VIRTUAL) {
            return Executors.newVirtualThreadPerTaskExecutor();
        }
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
