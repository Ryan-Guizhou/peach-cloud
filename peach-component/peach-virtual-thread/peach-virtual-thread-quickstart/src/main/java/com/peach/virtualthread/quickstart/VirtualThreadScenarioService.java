package com.peach.virtualthread.quickstart;

import com.peach.virtualthread.annotation.VirtualGroup;
import com.peach.virtualthread.api.VirtualExecutorService;
import com.peach.virtualthread.registry.VirtualExecutorRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * 常见 peach-cloud 阻塞 I/O 场景示例。
 *
 * <p>示例同时演示三种入口：直接使用 {@link VirtualExecutorService#submit(java.util.concurrent.Callable)}、
 * Starter 受管 CompletableFuture，以及按组路由的 {@link VirtualExecutorRegistry}。真实项目中将
 * lambda 内占位逻辑替换为 Mapper、StorageTemplate、OpenFeign、SMTP SDK 等阻塞调用即可。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:45
 */
@Service
public class VirtualThreadScenarioService {

    private final VirtualExecutorService databaseExecutor;
    private final VirtualExecutorService storageExecutor;
    private final VirtualExecutorService remoteExecutor;
    private final VirtualExecutorRegistry executorRegistry;

    /**
     * 创建示例服务。
     *
     * @param databaseExecutor database 业务组执行器
     * @param storageExecutor storage 业务组执行器
     * @param remoteExecutor remote 业务组执行器
     * @param executorRegistry 按业务组路由的执行器注册中心
     */
    public VirtualThreadScenarioService(
            @VirtualGroup("database") VirtualExecutorService databaseExecutor,
            @VirtualGroup("storage") VirtualExecutorService storageExecutor,
            @VirtualGroup("remote") VirtualExecutorService remoteExecutor,
            VirtualExecutorRegistry executorRegistry) {
        this.databaseExecutor = databaseExecutor;
        this.storageExecutor = storageExecutor;
        this.remoteExecutor = remoteExecutor;
        this.executorRegistry = executorRegistry;
    }

    /**
     * 使用标准 submit 方式模拟 MyBatis/JDBC 阻塞查询。
     *
     * @return 标准 Future；调用方式与传统 ExecutorService 一致
     */
    public Future<String> queryDatabaseWithSubmit() {
        return databaseExecutor.submit(() -> "database-result");
    }

    /**
     * 使用注册中心按组提交任务，调用方式接近老版 ThreadPoolManager。
     *
     * @return database 业务组返回的 Future
     */
    public Future<String> queryDatabaseWithRegistry() {
        return executorRegistry.submit("database", () -> "database-result");
    }

    /**
     * 使用 Starter 受管 CompletableFuture 模拟 MyBatis/JDBC 阻塞查询。
     *
     * @return 可组合、可取消的受管 CompletableFuture
     */
    public CompletableFuture<String> queryDatabase() {
        return databaseExecutor.supplyAsync(() -> "database-result");
    }

    /**
     * 模拟 OSS/S3/SFTP/NAS 阻塞调用。
     *
     * @return storage 业务组异步结果
     */
    public CompletableFuture<String> queryStorage() {
        return storageExecutor.supplyAsync(() -> "storage-result");
    }

    /**
     * 模拟 OpenFeign/阻塞 HTTP 调用。
     *
     * @return remote 业务组异步结果
     */
    public CompletableFuture<String> queryRemote() {
        return remoteExecutor.supplyAsync(() -> "remote-result");
    }

    /**
     * 在编排层并行执行不同资源组，再组合结果。
     *
     * @return 三个资源组组合后的异步结果
     */
    public CompletableFuture<String> aggregate() {
        CompletableFuture<String> database = queryDatabase();
        CompletableFuture<String> storage = queryStorage();
        CompletableFuture<String> remote = queryRemote();
        return database.thenCombine(storage, (db, file) -> db + ":" + file)
                .thenCombine(remote, (partial, service) -> partial + ":" + service);
    }
}
