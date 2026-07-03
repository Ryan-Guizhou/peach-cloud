# peach-common

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-common`  
类型：公共基础模块

## 模块定位

`peach-common` 是后端模块共享的基础依赖，承载公共常量、响应模型、异常体系、基础对象、工具类和跨模块复用代码。

本模块解决：

- 统一响应、异常和基础模型。
- 跨业务域共享的工具和常量。
- 降低业务模块之间的重复基础代码。

本模块不解决：

- 具体业务域逻辑。
- 中间件自动配置。
- Web、Gateway、MQ、Storage 等专项能力。

## 使用方式

业务模块通过 Maven 依赖使用：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-common</artifactId>
    <version>${revision}</version>
</dependency>
```

在当前仓库内，该依赖由根 `pom.xml` 的 `dependencyManagement` 统一管理，业务模块通常不需要显式写版本。

## 目录结构

```text
peach-common
├── pom.xml
└── src/main/java
```

说明：

- `peach-common` 是普通 jar 模块，不是启动模块。
- 不应在该模块中引入具体业务域反向依赖。
- 不应把需要外部中间件连接的能力直接放入 common。

## 设计约束

- 保持基础模块稳定，避免频繁修改公共 API 造成大范围影响。
- 公共异常、响应对象和工具类需要兼容多个业务域。
- 新增工具前先确认是否已有 JDK、Spring、Hutool 或 Apache Commons 能力可复用。
- 不在公共模块中写入业务硬编码、服务名、数据库表名或接口路径。

## 构建与验证

```bash
mvn -f "peach-common/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-common -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 下游模块编译失败 | 是否修改了公共类签名、泛型或包名 | 查找调用方并保持兼容，必要时新增方法而非直接删除 |
| 出现循环依赖 | common 是否依赖了业务域模块 | 移除反向依赖，把业务逻辑放回业务模块 |
| 工具类行为不符合某业务 | 工具是否过度承载业务语义 | 将业务规则迁回对应业务域 |
| 版本解析失败 | 是否从根目录构建；`${revision}` 是否生效 | 使用根 Maven 构建或加 `-am` |
