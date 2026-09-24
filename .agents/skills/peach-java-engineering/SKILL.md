---
name: peach-java-engineering
description: Peach Cloud Java 工程规范。修改或审查 Java 源码、公共 API、Spring Bean、事务、资源生命周期、Javadoc、日志、命名与基础代码结构时使用。
---

# Peach Java Engineering

## Baseline

- Java 21 / `jakarta.*`；框架 API 必须符合当前 POM 锁定版本。
- Spring Bean 使用构造器注入，依赖字段使用 `private final`；单构造器优先 `@RequiredArgsConstructor`。
- REST 负责协议适配，Service 负责业务/事务，DAO/XML 负责持久化，Entity/DTO/QO/VO 表达明确模型职责。
- 业务模块优先依赖 starter，不直接耦合 autoconfigure 或厂商 SDK。

## Javadoc And Logs

- Javadoc 使用中文；日志消息使用英文。
- Class、Interface、Enum、Annotation、Record 的类型级 Javadoc 保留：
  - `@Author Mr Shu`
  - `@Version 1.0.0`
  - `@CreateTime yyyy/M/d HH:mm`
- 公共 API、SPI、配置对象、复杂并发/事务/资源方法必须说明契约、边界、线程安全、阻塞行为、异常、生命周期、副作用和所有权。
- 不写仅翻译方法名的机械注释。
- 日志只记录排障所需非敏感白名单字段，不直接打印完整 DTO、Command、请求或响应对象。

## Correctness

- 事务边界放在可被 Spring 代理的业务入口，避免类内自调用导致事务/切面失效。
- Stream、client、executor、临时文件、锁等资源必须有明确所有权和关闭路径。
- 并发代码必须明确取消、中断、超时、拒绝、关闭和异常路径。
- 公共签名、配置、序列化模型、数据库映射和 XML `id` 变化必须检查调用方和兼容性。
- 不为了“现代”机械使用 Optional、Stream、record、var 或新 API。

## Naming And Scope

- 新代码使用完整、稳定、可搜索的英文标识符；历史拼写错误只做兼容。
- 一个类只承担一个清晰职责。
- 常量、异常、枚举和配置项表达业务语义，避免 magic string / number。
- 修改受影响文件时按本规范整理，但不要把无关行为重构混入同一变更。
