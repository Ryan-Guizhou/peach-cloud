# peach-initialize

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-initialize`  
类型：初始化组件聚合模块

## 模块定位

`peach-initialize` 提供应用启动后的初始化处理器编排能力。业务模块可以实现统一接口，把缓存预热、默认数据检查、外部资源探测等启动期动作纳入统一执行链。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-initialize-autoconfigure` | 初始化接口、自动配置和组合执行 |
| `peach-initialize-starter` | 对业务模块暴露的 starter |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `InitializeHandler` | 初始化处理器接口 |
| `InitializeHandlerType` | 初始化处理器类型常量 |
| `InitializeAutoConfig` | 自动配置入口 |
| `CompositeAutoConfig` | 组合初始化配置 |

## 接入方式

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-initialize-starter</artifactId>
</dependency>
```

业务模块实现 `InitializeHandler` 并注册为 Spring Bean。具体执行顺序、过滤条件和异常策略应以当前 autoconfigure 实现为准。

## 适用场景

- 启动后预热本地或分布式缓存。
- 检查必要配置或外部资源是否存在。
- 初始化默认数据或本地索引。
- 注册运行期处理器或刷新内存映射。

## 边界与限制

- 初始化动作会影响启动时间，不应执行无限等待或不可控大任务。
- 初始化不替代数据库迁移工具，不建议在这里执行复杂 DDL。
- 多实例启动时要考虑重复执行、并发执行和幂等。
- 初始化失败是阻断启动还是降级运行，需要由处理器和配置明确。

## 构建与验证

```bash
mvn -f "peach-component/peach-initialize/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-component/peach-initialize -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 初始化处理器未执行 | 是否引入 starter；处理器是否注册为 Bean | 检查依赖和 Spring Bean |
| 启动很慢 | 初始化任务是否阻塞或访问慢资源 | 给外部调用设置超时，拆分重任务 |
| 多实例重复初始化 | 处理器是否幂等；是否需要分布式锁 | 增加幂等记录或锁保护 |
| 初始化失败导致启动失败 | 异常策略是否符合预期 | 区分强依赖初始化和可降级初始化 |
