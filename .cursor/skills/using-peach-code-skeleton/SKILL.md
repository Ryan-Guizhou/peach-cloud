---
name: using-peach-code-skeleton
description: 约束 peach-cloud 的 REST、Entity、DAO/XML、Service、common 分层代码编写与审查；以 Java 21 为唯一后端语言基线，要求保留既有业务行为并执行安全、兼容性、UTF-8、构建和测试门禁。
---

# Peach Code Skeleton

## Workflow

1. 判断任务层级：`rest`、`entity`、`dao.xml`、`service`、`common`，只读取对应 reference。
2. Java 任务同时应用 `09-java21-modern-style`；历史代码只用于确认兼容行为，不作为新代码模板。
3. 修改前记录行为基线：输入、输出、异常、事务、权限、SQL、缓存、消息、序列化和关键日志/指标。
4. 修改公共 API、DAO/Service 签名、XML `id`、DTO/VO、Response、Starter SPI、缓存键或 MQ Event 前评估影响面。
5. 先处理安全、正确性、行为兼容和模块边界，再做 Java 21 风格现代化。
6. 完成后运行 UTF-8 无 BOM、受影响模块编译/测试和差异检查。

## Precedence

用户最新要求 > 安全与正确性 > 既有业务行为保持 > `AGENTS.md` > rules > 本 skill > 模块局部约定 > 历史存量风格。

## Java 21 Defaults

- 新增 Bean 使用构造器注入和 `private final` 依赖；禁止新增字段注入。
- DTO/VO/Command/Event 在兼容允许时优先 `record`；MyBatis DO/PO 不机械 record 化。
- pattern matching / switch expression / text block / `java.time` / try-with-resources 优先于旧式等价写法。
- 固定不可变集合优先 `List.of/Set.of/Map.of`，对外快照优先 `copyOf`，但不得改变原有可变语义。
- JDK 标准库优先；不为简单 null、Base64、文件、时间等能力扩大工具库依赖。
- IO 并发优先评估 Virtual Threads，CPU 密集任务保留有界平台线程池，可靠长任务继续使用 MQ/调度系统。
- 生产代码禁止依赖 JDK 21 Preview/Incubator 特性。

## Compatibility Guards

未经影响分析和测试禁止机械执行：

- mutable DTO/VO -> record；
- raw Response -> generic/record Response；
- BeanUtils -> constructor/MapStruct；
- String state -> enum；
- mutable collection -> immutable collection；
- ThreadPoolExecutor -> Virtual Threads；
- ThreadLocal 上下文模型调整；
- Service 接口删除、公共类/方法/包名重命名。

## Forbidden

- 为“拟合历史风格”复制完整 DTO 日志、敏感字段响应、错误日志级别、无效注解、事务自调用、资源泄漏或跨层依赖。
- 为兼容 Java 8/11/17 新增降级写法。
- 未确认第三方库行为就修改注解、配置、默认值或 SPI。
- 一次任务涉及一组同类文件时只修改其中一个而留下半套契约。
- 使用系统默认编码读写中文文件，或产生 UTF-8 BOM。

## References

- REST：`references/rest.md`
- Entity / DO / DTO / QO / VO：`references/entity.md`
- DAO 与 MyBatis XML：`references/dao-xml.md`
- Service：`references/service.md`
- common / peach-common：`references/common.md`

涉及 README、RocketMQ、Storage、Threadpool、Redis、Email 时叠加对应专项 skill。

## Definition Of Done

- 原有业务能力与可观察契约保持不变，除非用户明确授权行为变更。
- 分层、签名、校验、DAO/XML 和文档联动完整。
- 未记录或返回敏感字段，日志使用显式非敏感字段白名单。
- Java 21 风格符合 `09-java21-modern-style`，不启用 Preview。
- `node scripts/check-utf8.mjs` 通过。
- `mvn -pl <affected-module> -am test` 或等价范围测试通过。
- `git diff --check` 通过，且未覆盖用户无关改动。
- 无法执行的门禁在最终回复中明确说明。