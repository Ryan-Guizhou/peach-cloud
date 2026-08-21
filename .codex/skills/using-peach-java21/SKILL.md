---
name: using-peach-java21
description: 规范 peach-cloud 的 Java 21 原生风格重构与新增代码。Use when modernizing Java code, replacing Java 8-era patterns, adopting records/pattern matching/switch expressions/standard-library APIs/virtual threads, or reviewing whether a change preserves existing behavior while using Java 21 idioms.
---

# Peach Java 21

## Goal

将 peach-cloud 从“可运行在 Java 21”提升为“按 Java 21 编程模型设计”，但**不得改变既有业务能力和外部契约**。

## Workflow

1. 先读取 `AGENTS.md`、`09-java21-modern-style` 和任务对应的分层/专项 Skill。
2. 识别行为基线：API/JSON、异常、事务、SQL、缓存键、MQ、鉴权、序列化、线程/限流语义。
3. 将改动划分为 Level A（机械保行为）、Level B（契约联动）、Level C（架构/性能）。
4. 优先完成 Level A；Level B/C 必须评估调用方并补足测试。
5. 不使用 JDK 21 Preview/Incubator 作为生产基础能力。
6. 完成后执行 UTF-8、diff、受影响模块测试。

## Preferred Java 21 Style

- DTO/VO/Command/Event/Value Object：兼容允许时优先 `record`。
- MyBatis DO/PO：依赖 mutable JavaBean 语义时保留普通 class。
- Spring Bean：构造器注入 + `private final`。
- 有限状态：enum；有限类型层级：可评估 sealed hierarchy。
- 分支：pattern matching + switch expression。
- 集合：明确不可变时 `of/copyOf/toList`；不得改变原有可变契约。
- 时间：`java.time`，超时/TTL 新 API 优先 `Duration`。
- 多行文本：text blocks。
- 标准库优先：`HttpClient`、`Path/Files`、`Base64`、`HexFormat`、`Objects` 等。
- IO 高并发：优先评估 Virtual Threads；CPU 任务继续使用有界平台线程池。

## Compatibility Checklist

以下任一项发生变化时不得称为“纯重构”：

- getter/setter 或构造器可见性；
- JSON 字段名、null 处理、集合可变性；
- MyBatis/Jackson/Validation/Swagger 反射语义；
- Response 泛型/继承结构；
- Bean 注入选择；
- 缓存 key、TTL、MQ payload/topic/tag；
- 事务传播、异常类型/消息；
- 并发队列、拒绝策略、限流和上下文传播。

## Forbidden

- 为使用新特性而改变行为。
- `record everything`、`stream everything`、`var everywhere`。
- 在未验证场景把传统线程池全部替换成 Virtual Threads。
- 使用 `--enable-preview`。
- 在 `synchronized` 临界区执行阻塞 IO。

## Verification

```bash
node scripts/check-utf8.mjs
git diff --check
mvn -pl <affected-module> -am test
```

公共 API、Starter、SPI、上下文和并发模型改动需要扩大验证范围。