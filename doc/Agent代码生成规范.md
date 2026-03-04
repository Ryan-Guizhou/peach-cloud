# Peach Cloud Agent 代码生成规范（V1）

> 目标：让 Agent 在本仓库自动生成代码时，风格与现有工程保持一致，减少返工。

## 1. 通用约束
- 语言与注释：业务注释优先中文，关键技术点可追加英文短语（与现有代码风格一致）。
- JDK 版本：统一 Java 8，禁止生成 Java 9+ 语法（如 `var`、`List.of`、文本块）。
- 框架基线：Spring Boot 2.7.x + Spring Cloud 2021.x。
- 返回体规范：Controller 层优先使用 `com.peach.common.response.Response` 作为统一响应。
- 兼容优先：新增能力尽量以“增量接口/增量类”实现，避免破坏现有调用路径。

## 2. 包结构规范
- 根包统一使用：`com.peach.<module>`。
- 分层建议（按模块裁剪）：
  - `entity`：DTO/VO/实体对象
  - `service`：服务接口
  - `service.impl`：服务实现
  - `rest`：控制器
  - `config`：配置类
  - `launch`：启动类与运行期入口
- 禁止将其它微服务包名直接复制到当前模块（例如 monitor 模块中出现 userservice 包名）。

## 3. 命名规范
### 3.1 类命名
- Controller：`xxxController`
- Service 接口：`IxxxService` 或 `IxxxRuntimeService`
- Service 实现：`xxxServiceImpl`
- 配置类：`xxxConfig` 或 `xxxConfigure`
- DTO/VO：`xxxDTO` / `xxxVO`

### 3.2 方法命名
- 使用小驼峰 + 动词开头：`getRoleInfo`、`snapshot`、`updateTimeout`。
- Controller 方法名表达“动作 + 业务对象”，避免 `doSomething`、`test1` 这类弱语义命名。

### 3.3 变量命名
- 局部变量：小驼峰，语义清晰，避免 `obj`、`tmp`、`data1`。
- 常量：全大写 + 下划线分割。
- 集合变量优先复数或语义后缀：`userList`、`healthInfo`。

## 4. Controller 生成约束
- 每个接口补充 `@Operation(summary = "...")`，与 Knife4j/OpenAPI 对齐。
- 路由遵循资源语义，避免同义重复路径。
- 参数必须显式注解（`@PathVariable`、`@RequestParam`、`@RequestBody`）。
- 对外返回对象建议使用 `Response.success(...)` / `Response.fail(...)`。

## 5. Service 生成约束
- 先定义接口，再落实现类。
- 实现类仅承载业务逻辑，不做协议层处理（如 `HttpServletRequest` 解析）。
- 可复用 JDK 原生能力时不额外引入第三方依赖。

## 6. 配置类与安全相关约束
- Sa-Token、OpenAPI、Redis 等配置放置在 `<module>.service.config`。
- 配置类使用 `@Configuration`，避免在配置中写业务流程代码。
- 日志输出应包含模块标识和关键上下文，不输出敏感信息。

## 7. 日志与异常
- 使用 `@Slf4j`，日志按 `info/warn/error` 分级。
- 业务可预期失败使用统一 `Response` 语义表达；异常交由全局异常处理器兜底。
- 禁止吞异常；至少记录 `message + stacktrace`。

## 8. 依赖与模块边界
- 模块依赖遵循“最小依赖”原则，禁止把无关 starter 引入 monitor 模块。
- 复用跨模块能力时优先 external/openfeign 接口，不直接依赖他模块内部实现。

## 9. Agent 执行清单（提交前）
1. 新增/修改类的包名是否属于当前模块。
2. Controller 是否统一返回 `Response`。
3. 是否补充 OpenAPI 注解。
4. 是否存在 Java 8 不兼容语法。
5. 是否执行最小构建验证（至少 `mvn -pl <module> -am -DskipTests compile`）。

## 10. 本次落地说明
- 本规范根据当前仓库既有结构（`com.peach` 多模块、`Response` 统一返回、OpenAPI 注解使用方式）抽取。
- 后续如需增强（如 MyBatis 命名、Mapper XML 规则、前端 TypeScript 规则）可继续扩展到 V2。
