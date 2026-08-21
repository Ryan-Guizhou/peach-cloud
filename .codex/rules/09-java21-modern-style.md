# Java 21 Modern Style

本规则用于所有 peach-cloud Java 后端新增、修改和重构任务。目标不是“使用新语法”，而是在**不改变既有业务能力与契约**的前提下，采用 Java 21 的类型系统、不可变数据模型、标准库和并发模型。

## 1. Behavior First

- 重构前先识别输入、输出、异常、事务、SQL、缓存、MQ、鉴权、序列化和日志/指标中的可观察行为。
- 未经用户授权不得改变 HTTP API、JSON 字段、数据库结构/条件、缓存键、Topic/Tag、权限判断、事务边界和错误语义。
- “代码更现代”不能作为改变行为的理由。

## 2. Data Model

- DTO、VO、Command、Event、Value Object：框架与调用链兼容时优先 `record`。
- DO/PO/MyBatis Entity：若依赖无参构造、Setter、动态填充或反射，保留普通 class。
- `record` 的集合成员在 compact constructor 中优先 `List.copyOf/Set.copyOf/Map.copyOf`；必须保留可变语义时不得强制不可变。
- 不因 JSON 序列化而机械 `implements Serializable`；只有明确 Java Serialization/第三方协议要求时才实现。

## 3. Dependency Injection

- 新增 Spring Bean 默认构造器注入，依赖字段使用 `private final`。
- 禁止新增字段级 `@Resource` / `@Autowired`。
- 迁移存量字段注入时必须确保 Bean 选择语义不变；存在 qualifier/name 注入时显式保留限定信息。

## 4. Language

- 类型判断优先 `instanceof Type value`，禁止新增 `instanceof` 后再强转。
- 多分支值计算优先 switch expression；有限 enum/sealed hierarchy 尽量不写吞掉新类型的 `default`。
- 有限状态使用 enum；有限类型层级可评估 sealed interface/class。
- `var` 仅用于右侧能清晰看出类型的局部变量，不得降低可读性。
- 多行 SQL/JSON/HTML/Prompt 优先 text block。
- 不为了“现代”把清晰的业务 `for` 强制改成复杂 Stream。

## 5. Collections

- 新建固定不可变集合优先 `List.of/Set.of/Map.of`。
- 对外不可变快照优先 `copyOf`。
- Stream 简单转换优先 `.toList()`。
- JDK 21 Sequenced Collections 的 `getFirst/getLast/reversed` 仅在原集合顺序语义明确时使用。
- 若原调用方会修改集合，禁止机械替换为不可变集合。

## 6. Standard Library First

JDK 21 能清晰完成的通用能力优先标准库：

- `java.time` / `Duration`
- `java.net.http.HttpClient`
- `Path` / `Files`
- `Base64` / `HexFormat`
- `Objects`
- `Optional`（仅作为可能缺失的返回值，不作为字段/参数滥用）

不要为简单 null 判断、Base64、文件读取、字符串空白判断继续扩大 Hutool/Commons 依赖。

## 7. Concurrency

- 阻塞式 IO 高并发优先评估 Virtual Threads。
- CPU 密集计算使用有界 Platform Thread Pool。
- 长耗时、需要可靠重试/削峰/持久化的异步任务继续使用 RocketMQ/任务调度系统。
- Virtual Thread 不替代 HikariCP、Redis pool、HTTP connection pool、Sentinel、Semaphore、RateLimiter 等资源边界。
- 禁止在 `synchronized` 临界区执行 DB/Redis/HTTP/File 等阻塞 IO。
- 不在业务代码随手 `new Thread` 或创建未托管 Executor。

## 8. Preview Policy

生产代码禁止依赖 `--enable-preview`。JDK 21 中仍处 Preview/Incubator 的能力不得成为公共 API 或基础设施强依赖，包括但不限于 ScopedValue、Structured Concurrency、String Templates。

## 9. Migration Levels

### Level A - Mechanical / Behavior Preserving

可优先执行：

- diamond operator / 泛型补全
- 构造器注入（Bean 选择语义不变）
- pattern matching for `instanceof`
- switch expression（结果与异常完全一致）
- `java.time` 等价 API
- try-with-resources
- text blocks
- 明确可证明等价的 JDK 标准库替换

### Level B - Contract-Aware

必须影响分析 + 调用方联动 + 测试：

- mutable DTO/VO -> record
- raw `Response` -> generic response
- BeanUtils -> constructor/MapStruct
- String state -> enum
- interface/implementation 结构调整
- 集合改为不可变

### Level C - Architecture-Aware

必须性能/稳定性验证：

- ThreadPoolExecutor -> Virtual Threads
- ThreadLocal 上下文模型调整
- async 返回模型调整
- GC 策略调整

## 10. Verification

每批 Java 21 重构至少执行：

```bash
node scripts/check-utf8.mjs
git diff --check
mvn -pl <affected-module> -am test
```

公共基础模块、Starter、SPI、Response、上下文或并发模型改动需要扩大测试范围。