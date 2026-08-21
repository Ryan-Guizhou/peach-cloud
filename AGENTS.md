# peach-cloud Agent Guidelines

本文件是仓库级入口，规定 peach-cloud 的技术基线、变更边界、Rules/Skills 路由和质量门禁。历史代码仅用于理解兼容行为，不作为新代码风格依据。

> **最高原则**：任何现代化重构都不得改变既有业务能力、接口语义、鉴权逻辑、缓存键、消息协议、数据库契约、序列化字段或可观察行为，除非用户明确授权行为变更。

## Agent Native Configuration

- Cursor 使用 `.cursor/rules/*.mdc`、`.cursor/skills/*/SKILL.md` 和 `.cursor/mcp.json`。
- Codex 使用 `.codex/rules/*.md`、`.codex/skills/*/SKILL.md` 和 `.codex/config.toml`。
- 两套配置独立维护；修改通用规则时必须同步语义，不得只更新一侧。
- Java 后端任务必须同时应用 `06-layered-java-style` 与 `09-java21-modern-style`。

## Project Context

- 后端：Maven 多模块，**Java 21**，根 POM 使用 `maven.compiler.release=21`。
- Spring Boot：`3.5.4`；以根 POM 和 `peach-dependencies` 当前锁定版本为事实源。
- Spring Framework 6 / Jakarta 基线：新代码使用 `jakarta.*`，不得重新引入 `javax.*` 旧 API。
- 前端：`peach-cloud-front`，Vue 3 + Vite + TypeScript，独立 npm 工程，不属于 Maven reactor。
- 核心业务域：`peach-auth`、`peach-fileservice`、`peach-message`、`peach-setting`、`peach-generator`。
- 基础设施：`peach-common`、`peach-component`、`peach-middleware`、`peach-gateway`、`peach-monitor`、`peach-scheduled`。

## Precedence

冲突时按以下顺序处理：

1. 用户最新明确要求。
2. 安全、数据完整性、兼容性与可验证正确性。
3. **既有业务行为保持不变**。
4. 本文件。
5. 当前 Agent 原生 Rules。
6. 命中的 Skills。
7. 模块局部约定。
8. 历史存量风格。

## Java 21 Modernization Policy

### Required

- 新增 Java 代码必须以 Java 21 为唯一语言基线，不为 Java 8/11/17 保留降级写法。
- 默认不可变：DTO/VO/Command/Event/Value Object 在框架兼容和调用链允许时优先 `record`。
- DO/PO/MyBatis 持久化对象若依赖无参构造、Setter、动态填充或框架反射，继续使用普通 class，禁止为了使用 `record` 破坏映射契约。
- Service/Component/Configuration 默认构造器注入，依赖字段 `private final`；禁止新增字段级 `@Resource` / `@Autowired`。
- 有限状态优先 enum；有限类型层级优先评估 `sealed interface/class`。
- 类型分支优先 pattern matching 和 switch expression；禁止新增 `instanceof + 强制转换` 与传统 `switch + break`。
- 集合默认不可变边界：优先 `List.of/Set.of/Map.of`、`copyOf`、`Stream.toList()`；需要可变集合时显式创建。
- 时间统一使用 `java.time`；超时、TTL、租约等新 API 优先 `Duration`。
- JDK 标准库能清晰完成的能力优先标准库，避免为了 null 判断、Base64、文件读取等简单能力继续扩大 Hutool/Apache Commons 依赖。
- 阻塞 IO 并发优先评估 Virtual Threads；CPU 密集任务继续使用有界平台线程池；长耗时可靠异步任务继续使用 RocketMQ/任务系统。
- **禁止启用 Preview/Incubator 作为生产基础能力**，包括在 JDK 21 中仍为 Preview 的 ScopedValue、Structured Concurrency、String Templates 等。

### Compatibility Guard

下列改动在没有影响面分析和测试前禁止机械执行：

- `class DTO/VO -> record`：会改变 getter/setter、构造器、反射和 JavaBean 语义。
- `Response -> record/泛型 Response`：会改变公共 API、序列化及调用方类型。
- `ThreadLocal -> ScopedValue`：JDK 21 为 Preview，且会改变上下文传播方式。
- `ThreadPoolExecutor -> VirtualThread`：会改变限流、队列、拒绝策略和监控语义。
- `BeanUtils -> 构造器/MapStruct`：必须核对字段映射、审计字段、敏感字段。
- `Collections.emptyList()` 等替换为不可变集合：调用方若修改返回值会改变行为。
- Service 接口删除、类/方法改名、包名纠错：均属于公共符号变更，必须评估调用方。

## Standard Workflow

### 1. 修改前

1. 判断任务属于 REST、Entity/DTO/VO、DAO/XML、Service、common、middleware、component、gateway 或文档。
2. 加载对应 Rules；Java 任务必须加载 `06-layered-java-style` 和 `09-java21-modern-style`。
3. 命中项目 Skill 时完整读取对应 `SKILL.md`。
4. 修改公共 API、DAO/XML、Response、DTO/VO、Starter SPI、配置属性、缓存键、MQ Event、权限模型前必须做影响面分析。
5. 先记录行为基线：输入、输出、异常、事务、缓存、消息、SQL、日志/指标中的关键语义。

### 2. 修改中

- 优先做 100% 保行为的现代化：diamond operator、构造器注入、局部 pattern matching、switch expression、显式泛型、`java.time`、try-with-resources 等。
- 契约级现代化必须同步修改所有调用方和测试，禁止留下兼容半成品。
- Controller 只做绑定、校验、Service 调用和响应适配；业务编排、DAO、线程控制不得下沉到 Controller。
- Service 不得因“现代化”绕过原事务、权限、租户/机构过滤、逻辑删除、幂等、锁和审计逻辑。
- 不为追求 Stream/`var`/record 而降低可读性；现代 Java 的目标是更强类型、更少可变状态和更清晰生命周期。

### 3. 完成后

至少验证：

```bash
node scripts/check-utf8.mjs
git diff --check
mvn -DskipTests=false test
```

大型仓库可先运行受影响模块：

```bash
mvn -pl <module> -am test
```

若环境无法执行，最终必须明确未验证项，不得声称通过。

## Skill Routing

### Backend

| 修改范围 | 必用 Skill |
| --- | --- |
| REST、Entity、DAO/XML、Service、common | `using-peach-code-skeleton` |
| Java 21 语言/API/并发风格重构 | `using-peach-java21` + `using-peach-code-skeleton` |
| RocketMQ | `using-peach-code-skeleton` + `using-peach-rocket` |
| Storage | `using-peach-code-skeleton` + `using-peach-storage` |
| Threadpool / async / context propagation | `using-peach-code-skeleton` + `using-peach-threadpool` |
| Redis / Redisson | `using-peach-code-skeleton` + `using-peach-redis` |
| Email | `using-peach-code-skeleton` + `using-peach-email` |
| README / module docs | `using-peach-readme-writer` |

### Frontend

- Vue 3 / TS / Vite / Pinia / Router / Ant Design Vue：`using-peach-front`。
- 视觉/交互：按任务叠加 `ui-ux-pro-max`、`design-system` 或 `ui-styling`。

## Hard Forbidden

- 为保持历史风格新增 Java 8 写法。
- 未经验证改变 HTTP 路径、字段名、状态码、缓存键、Topic、Tag、SQL 条件、事务边界、权限判断或异常语义。
- 无实际需求让 DTO/VO `implements Serializable`。
- 新增字段注入。
- 新增 `new Thread(...)` 或游离 Executor。
- 在 Virtual Thread 场景下把外部资源并发误认为“无限并发”；数据库、Redis、HTTP、第三方 API 仍必须受连接池/Sentinel/Semaphore 等边界约束。
- 在 `synchronized` 临界区执行数据库、Redis、HTTP、文件等阻塞 IO。
- 新增原始类型 `Response` / `List` / `Map` 等 raw type。
- 记录密码、token、secret、完整认证 DTO 等敏感信息。

## Definition Of Done

- 原有业务能力和可观察契约保持不变，除非用户明确批准变化。
- Java 21 新代码符合 `09-java21-modern-style`。
- Rules、Skills、README 与源码事实一致。
- UTF-8 无 BOM、构建/测试、差异检查按任务范围完成。
- 无法执行的验证项必须明确记录。