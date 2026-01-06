package com.peach.threadpool.config;

import com.peach.threadpool.constant.ThreadConstant;
import com.peach.threadpool.core.PoolType;
import com.peach.threadpool.core.RejectedPolicy;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/4 19:02
 * @Description
 * 线程池配置属性类
 * 用于定义和配置线程池的各项参数
 */
public class PoolProperties {

    /**
     * 线程池类型
     * 根据任务特性选择合适的线程池类型，影响默认参数设置
     *
     * CPU: CPU密集型任务，线程数≈CPU核心数
     * IO: IO密集型任务，线程数≈CPU核心数×2~5
     * CACHED: 弹性缓存线程池，适用于短生命周期任务
     * SCHEDULED: 定时任务线程池
     *
     * 默认值：CPU（适用于通用计算场景）
     */
    private PoolType type = PoolType.CPU;

    /**
     * 核心线程数
     * 线程池中始终保持存活的线程数量，即使它们处于空闲状态
     *
     * 设置原则：
     * - CPU密集型：CPU核心数或+1
     * - IO密集型：CPU核心数×2~5
     * - 混合型：根据实际负载调整
     *
     * 默认值：CPU核心数和2之间的较大值
     * 保证至少有2个核心线程，避免单点瓶颈
     */
    private int coreSize = Math.max(2, Runtime.getRuntime().availableProcessors());

    /**
     * 最大线程数
     * 线程池允许创建的最大线程数量
     * 当队列满且核心线程都在忙时，会创建新线程直到达到此限制
     *
     * 设置原则：
     * - 一般为核心线程数的2~4倍
     * - 需考虑系统资源限制（内存、CPU）
     * - 过高会导致频繁上下文切换，降低性能
     *
     * 默认值：CPU核心数×4，且不小于核心线程数
     * 为突发流量提供缓冲能力
     */
    private int maxSize = Math.max(coreSize, Runtime.getRuntime().availableProcessors() * 4);

    /**
     * 空闲线程存活时间（秒）
     * 非核心线程在空闲状态下的最大存活时间
     * 超过此时间仍未执行任务的非核心线程将被回收
     *
     * 设置原则：
     * - 短时间：适用于任务频繁的场景（10-30秒）
     * - 长时间：适用于任务稀疏但需要快速响应的场景（60-300秒）
     * - 0：表示立即回收（谨慎使用，可能导致频繁创建线程）
     *
     * 默认值：60秒，平衡资源占用和响应速度
     */
    private long keepAliveSeconds = 60;

    /**
     * 队列容量
     * 工作队列的最大容量，用于缓冲待执行任务
     *
     * 设置原则：
     * - 小队列（<100）：快速响应，拒绝过载
     * - 中等队列（100-1000）：平衡响应和吞吐
     * - 大队列（>1000）：高吞吐，但可能增加延迟
     * - 0或负数：使用同步队列（SynchronousQueue）
     *
     * 默认值：1000，提供合理的缓冲能力
     * 注意：无界队列（Integer.MAX_VALUE）可能导致内存溢出
     */
    private int queueCapacity = 1000;

    /**
     * 是否允许核心线程超时回收
     * 为true时，核心线程在空闲超过keepAliveSeconds后也会被回收
     * 为false时，核心线程会一直存活（即使空闲）
     *
     * 使用场景：
     * - true：需要动态缩容的场景，节省资源
     * - false：需要保持快速响应的场景，避免线程创建开销
     *
     * 默认值：false，保持核心线程常驻以保证响应速度
     */
    private boolean allowCoreThreadTimeOut = false;

    /**
     * 线程名称前缀
     * 线程池创建的线程名称前缀，便于日志追踪和问题排查
     *
     * 命名规范建议：
     * - 包含业务标识：如"order-", "payment-"
     * - 包含环境标识：如"prod-", "test-"
     * - 保持简洁明了
     *
     * 线程名称格式：{prefix}{thread-num}
     * 示例："tp-order-1", "tp-order-2"
     *
     * 默认值："tp-"（thread-pool的缩写）
     */
    private String threadNamePrefix = ThreadConstant.THREAD_POOL_NAME_PREFIX;

    /**
     * 拒绝策略
     * 当线程池和队列都满时，对新提交任务的处理策略
     *
     * 可选策略：
     * - ABORT（默认）：抛出RejectedExecutionException
     * - CALLER_RUNS：由调用者线程直接执行
     * - DISCARD：静默丢弃任务
     * - DISCARD_OLDEST：丢弃队列中最旧的任务，然后重试
     *
     * 选择建议：
     * - 关键任务：CALLER_RUNS（确保任务被执行）
     * - 非关键批量任务：DISCARD_OLDEST
     * - 需要快速失败的场景：ABORT
     * - 可丢失的数据同步：DISCARD
     *
     * 默认值：CALLER_RUNS，避免数据丢失
     */
    private RejectedPolicy rejectedPolicy = RejectedPolicy.CALLER_RUNS;

    /**
     * 是否预启动核心线程
     * 为true时，线程池初始化时会立即创建所有核心线程
     *
     * 优点：
     * - 避免第一次请求时的线程创建延迟
     * - 提前预热线程池
     *
     * 缺点：
     * - 启动时占用更多资源
     * - 可能创建了不必要的线程
     *
     * 适用场景：
     * - 对响应时间要求极高的系统
     * - 已知会有突发流量的场景
     *
     * 默认值：false，按需创建以节省资源
     */
    private boolean prestartCoreThreads = false;



    public PoolType getType() {
        return type;
    }

    public void setType(PoolType type) {
        this.type = type;
    }

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public long getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(long keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public boolean isAllowCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public RejectedPolicy getRejectedPolicy() {
        return rejectedPolicy;
    }

    public void setRejectedPolicy(RejectedPolicy rejectedPolicy) {
        this.rejectedPolicy = rejectedPolicy;
    }

    public boolean isPrestartCoreThreads() {
        return prestartCoreThreads;
    }

    public void setPrestartCoreThreads(boolean prestartCoreThreads) {
        this.prestartCoreThreads = prestartCoreThreads;
    }


}