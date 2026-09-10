# peach-virtual-thread-quickstart

用于验证 `peach-virtual-thread-starter` 的虚拟线程分组、容量控制、背压、取消和关闭行为。

## 接入

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-virtual-thread-starter</artifactId>
</dependency>
```

启动类为 `com.peach.virtualthread.quickstart.PeachVirtualThreadQuickstartApplication`。`VirtualThreadScenarioService` 展示 `VirtualExecutorRegistry` 和 `@VirtualGroup` 的使用方式。

在 `application.yml` 中为每个阻塞资源配置 `max-concurrency`、`max-pending` 和背压策略。虚拟线程不替代数据库、HTTP、Redis 或对象存储的连接池容量限制，也不用于 CPU 密集型任务。

## 运行

```bash
mvn -f peach-component/peach-virtual-thread/peach-virtual-thread-quickstart/pom.xml spring-boot:run
```
