# peach-cloud Java 21 现代化改造指南

## 1. 目标

本改造的目标不是简单把编译版本升级到 Java 21，而是在**不改变既有业务能力、接口语义和运行结果**的前提下，将代码逐步迁移为 Java 21 原生风格。

核心原则：

1. Behavior First：行为兼容高于语法现代化。
2. Immutable by Default：适合的数据模型优先不可变。
3. Strong Typing：减少 `Object`、raw type、字符串状态和隐式 Bean 映射。
4. JDK First：标准库能清晰解决的问题优先 JDK 21。
5. Concurrency by Workload：IO、CPU、可靠异步、资源并发分别治理。
6. No Preview in Production：生产代码不依赖 JDK 21 Preview/Incubator。

---

## 2. 不允许改变的能力

除非专项需求明确授权，下列内容必须保持：

- HTTP path / method / request / response JSON 字段；
- 返回码、错误语义与关键异常类型；
- Sa-Token 登录、权限、租户和机构隔离逻辑；
- MyBatis SQL、逻辑删除、租户/机构条件和事务边界；
- Redis key、field、TTL、锁、Stream、Pub/Sub 语义；
- RocketMQ Topic、Tag、Key、payload、幂等和重试语义；
- Storage object key、路径、bucket/provider 路由和签名 URL 行为；
- Threadpool queue、rejection、timeout、async 和 context propagation 语义；
- Feign requestId / sameToken 等已有传播契约；
- 关键日志、指标、Tracing 中用于排障的业务标识。

---

## 3. 改造等级

### Level A：机械保行为

默认可优先实施，但仍需编译/测试：

- diamond operator；
- 构造器注入替代简单字段注入；
- `instanceof` pattern matching；
- 等价 switch expression；
- try-with-resources；
- text blocks；
- `java.time` 等价替换；
- 明确等价的 JDK 标准库 API；
- 明确不会被修改的内部固定集合使用 `of/copyOf`。

### Level B：契约联动

必须先影响分析，再同时修改调用方和测试：

- DTO/VO/Command/Event -> `record`；
- raw `Response` -> `Response<T>` / `ApiResponse<T>`；
- BeanUtils -> constructor / MapStruct；
- 字符串状态 -> enum；
- 集合可变 -> 不可变；
- Service 接口、类、包名和方法命名调整；
- Optional 返回模型调整；
- 配置属性 mutable bean -> immutable record。

### Level C：架构/性能

必须专项基准、稳定性和压力测试：

- IO ThreadPool -> Virtual Threads；
- ThreadLocal 上下文传播模型；
- `@Async` / `CompletableFuture` 返回模型；
- Web MVC 与异步执行模型；
- GC / ZGC；
- Feign/HTTP 并发模型；
- 文件处理和大批量任务执行模型。

---

## 4. Java 21 编码标准

### 4.1 DTO / VO / Command / Event

新模型优先：

```java
public record UserContext(
        String userId,
        String tenantId,
        String orgId,
        Set<String> permissions
) {
    public UserContext {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
```

但以下情况暂不 record 化：

- 调用方依赖 `getXxx/setXxx`；
- Jackson/Swagger/Validation/Feign 配置尚未验证；
- MyBatis 或 BeanUtils 依赖 JavaBean；
- 对象本身具有明确 mutable 生命周期。

### 4.2 DO / PO

MyBatis 持久化对象默认继续普通 class：

```java
@Getter
@Setter
public class UserDO extends PeachDO {
    private String userId;
}
```

不要为了 record 破坏无参构造、Setter、动态 SQL 和审计填充。

### 4.3 Dependency Injection

新代码：

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;
    private final StringRedisTemplate redisTemplate;
}
```

禁止新增字段级 `@Resource` / `@Autowired`。

### 4.4 Pattern Matching

```java
if (value instanceof UserVO user) {
    return user.getUserId();
}
```

### 4.5 Switch Expression

```java
return switch (type) {
    case API -> handleApi();
    case BUTTON -> handleButton();
};
```

有限 enum/sealed 类型尽量不写无意义 `default`，让新增类型触发编译错误。

### 4.6 Collections

- 固定不可变：`List.of` / `Set.of` / `Map.of`；
- 不可变快照：`copyOf`；
- 简单 Stream 结果：`.toList()`；
- JDK 21 顺序集合：`getFirst/getLast/reversed`。

如果调用方会修改集合，不允许机械替换。

### 4.7 Time

- 日期：`LocalDate`；
- 业务本地时间：`LocalDateTime`；
- 跨服务绝对时间：`Instant`；
- 带偏移时间：`OffsetDateTime`；
- timeout / TTL / lease：新 API 优先 `Duration`。

### 4.8 Standard Library First

优先评估：

- `java.net.http.HttpClient`；
- `Path` / `Files`；
- `Base64` / `HexFormat`；
- `Objects`；
- `java.time`；
- `Stream.toList()`。

Hutool/Commons 仍可保留复杂且成熟的既有能力，但不再作为简单 JDK 能力的默认入口。

---

## 5. Java 21 并发模型

### Blocking IO

候选：Virtual Threads。

适用：

- Feign/HTTP；
- JDBC 外围业务编排；
- OSS/S3/File IO；
- 第三方 API；
- 阻塞 SDK。

### CPU Bound

继续使用有界 Platform Thread Pool：

- 图片编解码；
- 压缩；
- SHA/加解密；
- 大 JSON/数据计算；
- 音视频处理。

### Reliable Async

继续使用 RocketMQ / Scheduler / Worker：

- 长耗时任务；
- 削峰；
- 可恢复任务；
- 必须可靠重试的任务。

### Resource Concurrency

Virtual Thread 不能替代：

- HikariCP；
- Redis connection pool；
- HTTP connection pool；
- Sentinel；
- Semaphore；
- RateLimiter。

---

## 6. 模块迁移顺序

### Phase 0：规则与门禁

- `AGENTS.md`；
- `.codex/rules` / `.cursor/rules`；
- 后端 Skills；
- UTF-8 / build / test / diff 门禁。

### Phase 1：peach-common

优先审查：

- `CurrentContext`；
- `Response` / `PageResult`；
- Exception；
- 时间、ID、字符串等公共工具；
- 基础 Entity/DAO 契约。

### Phase 2：peach-auth

作为业务迁移样板：

- field injection -> constructor injection；
- DTO/VO record 可行性；
- String permission/resource type -> enum；
- BeanUtils 显式映射；
- raw Response 泛型演进评估；
- 登录、租户、机构和权限行为回归测试。

### Phase 3：peach-component

重点：`peach-threadpool`、Email 等。

### Phase 4：peach-middleware

重点：Redis、Storage、RocketMQ、OpenFeign。

### Phase 5：业务模块

`peach-setting`、`peach-message`、`peach-fileservice`、`peach-generator`、`peach-scheduled`。

### Phase 6：gateway / monitor

- Gateway 保持 Reactor/Netty 模型，不因 Virtual Threads 强行 MVC 化；
- Monitor 补齐 Java 21 / Virtual Thread / executor / connection pool 指标。

---

## 7. 每个模块的迁移检查表

- [ ] API 输入输出是否完全一致；
- [ ] JSON 字段/null 语义是否一致；
- [ ] SQL 和事务是否一致；
- [ ] Redis key/TTL 是否一致；
- [ ] MQ 协议是否一致；
- [ ] 权限、租户、机构过滤是否一致；
- [ ] Bean 注入选择是否一致；
- [ ] 集合可变性是否被意外改变；
- [ ] 异常类型和关键错误信息是否一致；
- [ ] MDC/SecurityContext/CurrentContext 是否跨异步边界正确传播；
- [ ] 阻塞 IO 是否发生在 synchronized 临界区；
- [ ] Java 21 Preview 是否为 0；
- [ ] UTF-8 无 BOM 检查通过；
- [ ] 受影响模块测试通过；
- [ ] `git diff --check` 通过。

---

## 8. 禁止清单

新代码禁止：

- 字段级 `@Resource` / `@Autowired`；
- `new Thread(...)`；
- 未托管 Executor；
- `instanceof` 后手工 cast；
- 传统 `switch + break` 用于值计算；
- 无需求 DTO/VO `Serializable`；
- raw `List/Map/Response`；
- 新业务状态继续使用裸字符串；
- 在锁内执行阻塞 IO；
- `--enable-preview`；
- 为追求新语法改变既有行为。

---

## 9. 验证命令

```bash
node scripts/check-utf8.mjs
git diff --check
mvn -pl <affected-module> -am test
```

跨公共模块改造再执行全量：

```bash
mvn test
```

若运行环境不具备验证条件，提交说明必须明确标注未验证范围与风险。