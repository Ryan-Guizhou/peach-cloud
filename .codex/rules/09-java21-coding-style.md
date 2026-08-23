# Java 21 Coding Style

本规则与 `05-language-and-encoding`、`06-layered-java-style` 配合；冲突时 Java 21 语法与 Spring Boot 3.5.4 行为优先于 JDK 8 存量写法。

## Baseline

`REQUIRED`：

- 基线：Java 21、`maven.compiler.release=21`、Spring Boot `3.5.4`、Spring Cloud `2025.0.0`、Spring Cloud Alibaba `2025.0.0.0`、`jakarta.*`。
- Spring 管理的 Bean（`@Service`、`@Component`、`@RestController`、`@Controller`、`@Configuration`）使用构造器注入；依赖字段 `private final`。
- 禁止新代码使用字段 `@Resource` 或 `@Autowired`；存量触达时必须迁移。
- 单构造器 Bean 使用 Lombok `@RequiredArgsConstructor`；多 Bean 同类或 `@Qualifier` 场景使用显式构造器。

## Language Features

### REQUIRED（触达文件时必须采用）

| JDK 8 写法 | JDK 21 写法 |
| --- | --- |
| `collect(Collectors.toList())` | `.toList()`（后续需修改则 `new ArrayList<>(stream.toList())`） |
| `Collections.emptyList()` / `singletonList`（只读） | `List.of()` |
| `Collections.emptyMap()`（只读） | `Map.of()` |
| `!optional.isPresent()` | `optional.isEmpty()` |
| `str == null \|\| str.trim().isEmpty()` | `str == null \|\| str.isBlank()` |
| `(Type) obj` + `instanceof` | `obj instanceof Type t` |
| 多行 `+` 拼接 SQL/Lua/JSON | text block |
| `new File(path)` 读文本 | `Files.readString(Path.of(path))` |

### PREFERRED（可读性优先）

- `var`：仅用于局部变量且 RHS 类型明确；禁止用于字段、方法签名、返回值、`null` 字面量。
- `switch` 表达式：分支清晰且能降低复杂度时使用。
- switch 模式匹配（Java 21）：类型分支明确时使用。

### record 决策树

1. MyBatis/JPA 持久化 `DO extends PeachDO` → **class + Lombok**（不变）
2. 带 JSR-303 分组（`PeachGroup` 派生）的 DTO → **class**（暂保留）
3. `VO extends DO` 存量 → **class**（暂保留）
4. 纯不可变数据传输（MQ Event、内部 Value Object、Feign 只读响应）→ **record**
5. 封闭状态/事件变体 → **sealed interface + record 实现**

### FORBIDDEN

- 全仓库机械 `var` 或全仓库 `record` 化
- DO/持久化实体改 record
- 用虚拟线程替换 Redisson delay queue、Quartz、`PoolType.CPU` 平台线程池
- 在 `peach-gateway` WebFlux 模块启用 Servlet 虚拟线程配置

## Virtual Threads

| 层级 | 策略 |
| --- | --- |
| Servlet `*-launch` | `spring.threads.virtual.enabled: true` |
| `peach-gateway` WebFlux | 不配置 Servlet 虚拟线程 |
| `ThreadPoolManager` | 保留 CPU/IO/SCHEDULED 平台线程池；纯 IO fire-and-forget 可用 `PoolType.VIRTUAL` |
| `@Scheduled` / Quartz | 平台线程 |

## Verification

```bash
node scripts/check-utf8.mjs
git diff --check
mvn test -Pdevelopment
```

改造完成后 Spring Bean 中不应再出现字段 `@Resource` / `@Autowired`。
