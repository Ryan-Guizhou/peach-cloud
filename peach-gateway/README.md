# peach-gateway

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-gateway`  
类型：网关聚合模块

## 模块定位

`peach-gateway` 是 Peach Cloud 的统一流量入口，基于 Spring Cloud Gateway 承载请求路由、服务转发、网关侧认证集成和 API 聚合入口。

本模块解决：

- 网关启动和运行时路由入口。
- 统一承接前端和外部请求，再转发到后端业务服务。
- 组合 `peach-satoken` 网关侧能力实现认证上下文接入。
- 与 Knife4j 网关聚合等能力配合，为后端接口提供统一访问入口。

本模块不解决：

- 业务域权限数据维护，权限数据位于 `peach-auth`。
- 业务接口实现，接口实现位于各业务服务的 `*-rest` 模块。
- 生产级 WAF、DDoS、防刷、全链路限流和灰度发布治理。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-gateway-core` | 网关核心配置、过滤器和共享逻辑 |
| `peach-gateway-launch` | Spring Cloud Gateway 启动模块 |

## 关键入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-gateway-launch/src/main/java/com/peach/gateway/launch/PeachGatewayApplication.java` |
| 配置文件 | `peach-gateway-launch/src/main/resources/application.yml` |
| 环境配置 | `peach-gateway-launch/src/main/resources/application-dev.yml`、`application-docker.yml`、`application-prod.yml` |
| 核心模块 | `peach-gateway-core/src/main/java/com/peach/gateway/core` |

## 运行机制

1. `PeachGatewayApplication` 启动 Gateway 应用。
2. 网关读取本地 `application-*.yml` 和外部配置中心配置。
3. 请求进入 Gateway 过滤链，执行认证、上下文、路由匹配等逻辑。
4. 匹配到的请求转发到认证、文件、消息、配置、监控、生成器等后端服务。
5. 后端响应经网关返回给调用方。

## 配置说明

- 本地配置位于 `peach-gateway-launch/src/main/resources`。
- Docker Compose 中网关服务端口映射为 `18080`。
- 服务发现、路由规则、鉴权策略通常需要结合 Nacos 和运行环境配置确认。
- 网关配置中不要写入生产密钥、生产内网地址或不可公开的 Token。

## 边界与限制

- 网关只负责流量入口和路由转发，不持有业务数据最终解释权。
- 网关侧鉴权依赖认证服务、Sa-Token 配置、Redis 和路由白名单共同生效。
- 如果绕过网关直连业务服务，网关过滤器不会生效，业务服务仍需按需做服务侧保护。
- Docker Compose 配置适合本地联调，不应直接作为生产网关部署方案。

## 构建与验证

```bash
mvn -f "peach-gateway/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-gateway/peach-gateway-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-gateway/peach-gateway-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

Docker Compose 验证：

```bash
bin\start.bat up
bin\start.bat ps
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 前端请求 404 | 路由规则、服务名、路径前缀是否匹配 | 检查网关配置和后端服务注册状态 |
| 认证失败 | Token、Sa-Token 配置、Redis、白名单是否正确 | 先确认认证服务登录成功，再检查网关过滤链 |
| 服务转发超时 | 后端服务是否启动；Nacos 是否可用；网络是否连通 | 查看网关和目标服务日志 |
| Docker 网关无法访问后端 | 是否使用容器服务名；compose 网络是否一致 | Docker 内部访问不要使用宿主机 `localhost` |
| 聚合文档缺失 | Knife4j 网关配置和后端文档端点是否可访问 | 分别直连后端文档端点和网关聚合端点排查 |
