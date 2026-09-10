# peach-initialize

[English](README.en-US.md) | 中文

`peach-initialize` 用于组织应用启动期初始化处理器，例如缓存预热、必要资源检查和内存映射初始化。

## 结构

| 模块 | 职责 |
| --- | --- |
| `peach-initialize-autoconfigure` | `InitializeHandler` 契约、编排和自动配置 |
| `peach-initialize-starter` | 业务接入依赖入口 |
| `peach-initialize-quickstart` | 最小处理器接入验证 |

业务模块依赖：

```xml
<dependency><groupId>com.peach</groupId><artifactId>peach-initialize-starter</artifactId></dependency>
```

Quickstart：[`peach-initialize-quickstart`](peach-initialize-quickstart/README.md)。

## 边界

- 初始化任务必须有界，不能无限等待或承载不可控大任务。
- 初始化不替代数据库迁移工具。
- 多实例下需要明确重复执行、并发和幂等语义。
- 初始化失败是阻断启动还是降级运行必须由当前处理器契约和配置明确。
