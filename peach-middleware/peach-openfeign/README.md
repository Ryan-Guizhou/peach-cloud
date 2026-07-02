# Peach OpenFeign Starter

[English](README.en-US.md)

最后更新时间：2026/7/2  
维护人：Mr Shu  
适用版本：`peach-cloud 1.0.0-SNAPSHOT`、JDK 8、Spring Boot 2.7.x、Spring Cloud OpenFeign

## 目录

- [1. 模块概览](#1-模块概览)
- [2. 模块职责边界](#2-模块职责边界)
- [3. 模块结构总览](#3-模块结构总览)
- [4. 文件职责说明](#4-文件职责说明)
- [5. 默认行为](#5-默认行为)
- [6. 快速接入](#6-快速接入)
- [7. 完整配置清单](#7-完整配置清单)
- [8. 关键实现说明](#8-关键实现说明)
- [9. 自动装配说明](#9-自动装配说明)
- [10. 构建与验证](#10-构建与验证)
- [11. 排障指南](#11-排障指南)
- [12. 当前限制与建议](#12-当前限制与建议)

## 1. 模块概览

`peach-openfeign` 是 Peach Cloud 的 OpenFeign 公共 starter，用于统一服务间 Feign 调用时的公共请求头处理逻辑。

当前模块聚焦两件事：

- 为下游 Feign 请求自动注入 `Same-Token`
- 按规则透传当前 HTTP 请求头到下游服务

当前模块采用两段式结构：

- `peach-openfeign-autoconfigure`：自动装配、配置属性和拦截器实现
- `peach-openfeign-starter`：业务模块实际引入的 starter 聚合模块

该模块当前不负责这些内容：

- 不扫描具体 `@FeignClient`
- 不处理 Feign 降级、重试、超时、日志级别
- 不替代业务服务自己的鉴权逻辑
- 不负责 Gateway 到业务服务的 `Same-Token` 注入

## 2. 模块职责边界

### 2.1 模块提供的能力

- 统一注册 `RequestInterceptor`
- 自动跳过透传入站请求中的 `Same-Token`
- 为下游请求重新注入当前有效的 `Same-Token`
- 支持按配置开关控制 header relay 和 same-token 注入
- 支持统一排除不应透传的冲突请求头
- 支持多值请求头完整透传

### 2.2 模块不提供的能力

- 不保证异步线程、定时任务线程一定存在 Servlet 请求上下文
- 不保证业务自定义拦截器之间的顺序协调
- 不自动为非 HTTP 触发链路构造业务请求头
- 不负责跨服务请求路径映射和网关路由

### 2.3 与 `peach-satoken` 的关系

- `peach-satoken` 负责 Gateway 或普通服务上的 Sa-Token 鉴权与 Same-Token 校验
- `peach-openfeign` 负责服务作为调用方时，把请求头和 `Same-Token` 带给下游

推荐理解方式：

- 外部请求进入系统：`peach-satoken`
- 服务间 Feign 调用：`peach-openfeign`

## 3. 模块结构总览

```text
peach-middleware/peach-openfeign/
├── pom.xml
├── README.md
├── README.en-US.md
├── peach-openfeign-autoconfigure/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/peach/openfeign/
│       │   ├── autoconfigure/
│       │   │   └── PeachOpenFeignAutoConfiguration.java
│       │   └── config/
│       │       └── PeachOpenFeignProperties.java
│       └── resources/
│           └── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
└── peach-openfeign-starter/
    └── pom.xml
```

## 4. 文件职责说明

### 4.1 根目录文件

| 路径 | 作用 |
| --- | --- |
| `peach-middleware/peach-openfeign/pom.xml` | 聚合 POM，声明 `autoconfigure` 和 `starter` 两个子模块 |
| `peach-middleware/peach-openfeign/README.md` | 中文主文档 |
| `peach-middleware/peach-openfeign/README.en-US.md` | 英文主文档 |

### 4.2 `peach-openfeign-autoconfigure`

| 文件 | 作用 |
| --- | --- |
| `autoconfigure/PeachOpenFeignAutoConfiguration.java` | 注册 Feign `RequestInterceptor`，统一处理 header relay 和 same-token 注入 |
| `config/PeachOpenFeignProperties.java` | `peach.openfeign.*` 配置属性模型 |
| `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Spring Boot 自动装配入口声明 |

### 4.3 `peach-openfeign-starter`

| 文件 | 作用 |
| --- | --- |
| `peach-openfeign-starter/pom.xml` | 对业务模块暴露的 starter，聚合 OpenFeign 和自动装配依赖 |

## 5. 默认行为

引入 starter 后，默认会启用以下行为：

- 自动注册名为 `peachOpenFeignRequestInterceptor` 的 Feign 拦截器
- 默认开启 Same-Token 注入
- 默认开启当前 Servlet 请求头透传
- 默认跳过透传入站请求里的 `Same-Token`
- 默认排除这些不应透传的请求头：
    - `content-type`
    - `content-length`
    - `host`
    - `connection`
    - `keep-alive`
    - `proxy-connection`
    - `te`
    - `trailer`
    - `transfer-encoding`
    - `upgrade`
    - `accept-encoding`

## 6. 快速接入

### 6.1 引入依赖

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-openfeign-starter</artifactId>
</dependency>
```

通常业务服务不需要直接引入该 starter，而是通过各个 `*-openfeign-external` 模块间接引入。

### 6.2 最小配置

大多数服务不需要额外配置，默认即可工作：

```yaml
peach:
  openfeign:
    enabled: true
```

### 6.3 示例说明

当某个服务收到 HTTP 请求后，再通过 `MessageFeignClient` 调用 `peach-message`：

1. 拦截器读取当前 `ServletRequestAttributes`
2. 复制允许透传的请求头到下游 Feign 请求
3. 移除已有 `Same-Token`
4. 重新注入当前有效的 `Same-Token`

## 7. 完整配置清单

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.openfeign.enabled` | `true` | 是否启用自动装配 |
| `peach.openfeign.same-token-enabled` | `true` | 是否为下游 Feign 请求注入 `Same-Token` |
| `peach.openfeign.relay-headers` | `true` | 是否透传当前 Servlet 请求头 |
| `peach.openfeign.exclude-headers` | 见下方 | 需要排除的请求头名称列表，大小写不敏感 |

默认 `exclude-headers`：

```yaml
peach:
  openfeign:
    exclude-headers:
      - content-type
      - content-length
      - host
      - connection
      - keep-alive
      - proxy-connection
      - te
      - trailer
      - transfer-encoding
      - upgrade
      - accept-encoding
```

## 8. 关键实现说明

### 8.1 `Same-Token` 处理策略

当前实现不是简单透传入站请求里的 `Same-Token`，而是：

1. 在 header relay 阶段跳过 `Same-Token`
2. 在单独步骤里重新调用 `SaSameUtil.getToken()` 注入

这样可以保证下游收到的是当前服务视角下合法的 `Same-Token`，而不是上游残留值。

### 8.2 多值请求头透传

当前版本会完整保留多值 header，而不是只取第一个值。  
例如 `Accept`、`Accept-Language` 这类多值头不会再被截断。

### 8.3 非 Servlet 上下文行为

如果当前线程中没有 `ServletRequestAttributes`：

- 不会抛异常
- 不会透传 HTTP 请求头
- 如果开启了 `same-token-enabled`，仍会继续尝试注入 `Same-Token`

这适用于部分内部服务调用、非 Web 触发链路或测试场景。

### 8.4 为什么默认排除 hop-by-hop 头

像 `connection`、`transfer-encoding`、`upgrade` 这类头只对单段连接有效，继续透传到下游服务容易带来冲突或不可预期行为，因此被默认排除。

## 9. 自动装配说明

自动装配入口文件：

```text
peach-openfeign-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

注册类：

```text
com.peach.openfeign.autoconfigure.PeachOpenFeignAutoConfiguration
```

默认注册 Bean：

| Bean 名称 | 类型 | 作用 |
| --- | --- | --- |
| `peachOpenFeignRequestInterceptor` | `RequestInterceptor` | 统一处理 header relay 和 same-token 注入 |

生效条件：

- classpath 中存在 `RequestInterceptor`
- `peach.openfeign.enabled=true`
- 容器中没有同名 Bean `peachOpenFeignRequestInterceptor`

## 10. 构建与验证

模块级构建命令：

```bash
mvn -f "peach-middleware/peach-openfeign/pom.xml" -DskipTests compile
```

本次修改重点验证：

- `peach-openfeign-autoconfigure` 编译通过
- `peach-openfeign-starter` 编译通过
- 多值 header 透传实现可通过编译
- 新增默认排除头配置可通过编译

## 11. 排障指南

| 现象 | 检查点 |
| --- | --- |
| 下游服务 Same-Token 校验失败 | 检查当前服务是否引入 `peach-openfeign-starter`，并确认 `same-token-enabled=true` |
| 下游服务收到的语言头不完整 | 检查是否被自定义拦截器覆盖，当前模块已支持多值 header 透传 |
| 某些请求头不应继续传递 | 在 `exclude-headers` 中追加配置 |
| 非 Web 线程调用下游服务时拿不到业务请求头 | 这是当前设计行为，需要业务侧自行补充上下文或显式设置请求头 |
| 自定义 Feign 拦截器未生效或行为冲突 | 检查是否有其他 `RequestInterceptor` 覆盖或叠加同名 header |

## 12. 当前限制与建议

当前限制：

- 当前模块只处理 Servlet 请求上下文，不处理 Reactor 上下文
- header relay 只覆盖入站 HTTP 请求，不负责业务自建上下文透传
- 当前模块没有单独的自动化测试类

建议：

- 如果后续需要支持异步线程上下文透传，可以单独抽象请求头上下文提供器
- 如果后续要兼容 WebFlux 上游上下文，不建议继续直接依赖 `RequestContextHolder`
- 如果后续存在多个公共 Feign 拦截器，建议明确顺序与职责边界
