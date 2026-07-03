# peach-monitor

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-monitor`  
类型：监控与审计业务域聚合模块

## 模块定位

`peach-monitor` 提供运行时监控、审计查询和监控 REST 接口。它是业务系统观察运行状态、审计记录和监控数据的后端服务模块。

本模块解决：

- 监控域实体、公共对象和服务分层。
- 监控 REST API。
- 面向其他服务的 OpenFeign 外部接口。
- 独立启动和本地联调入口。

本模块不解决：

- Prometheus、Grafana、SkyWalking、ELK 等完整可观测平台部署。
- 日志采集 Agent、链路追踪 Agent 和告警平台。
- 所有业务域审计事件的自动生成。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-monitor-common` | 监控域公共对象和常量 |
| `peach-monitor-entity` | 监控、审计相关实体模型 |
| `peach-monitor-service` | 监控领域服务和数据访问 |
| `peach-monitor-rest` | 监控 REST 接口 |
| `peach-monitor-openfeign-external` | 面向其他服务的 OpenFeign 接口 |
| `peach-monitor-launch` | Spring Boot 启动模块 |

## 关键入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-monitor-launch/src/main/java/com/peach/monitor/launch/PeachMonitorApplication.java` |
| 配置文件 | `peach-monitor-launch/src/main/resources/application-dev.yml` |
| REST 控制器 | `peach-monitor-rest/src/main/java/com/peach/monitor/rest/MonitorController.java` |
| REST 前缀 | `/monitor` |
| 服务包 | `peach-monitor-service/src/main/java/com/peach/monitor/service` |

## 运行机制

1. `peach-monitor-launch` 启动监控服务。
2. REST 层接收监控和审计查询请求。
3. Service 层查询监控域数据并组织响应。
4. 其他服务可以通过 OpenFeign 外部接口写入或查询监控数据。
5. 实际日志、指标、链路追踪平台需要由外部可观测系统承接。

## 配置说明

- 启动配置位于 `peach-monitor-launch/src/main/resources/application-*.yml`。
- 数据库、Nacos、Redis 等基础配置按 profile 生效。
- 如果接入外部监控系统，应在部署层配置采集器、指标端点和告警规则。

## 边界与限制

- 本模块是监控业务域服务，不等同于完整可观测平台。
- 审计记录是否完整取决于业务服务是否产生并写入审计事件。
- 高吞吐审计写入需要关注数据库容量、索引和归档策略。
- 生产环境应对监控接口做权限控制，避免泄露运行信息。

## 构建与验证

```bash
mvn -f "peach-monitor/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-monitor/peach-monitor-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-monitor/peach-monitor-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 监控接口不可访问 | 服务是否启动；路径 `/monitor` 是否经网关路由 | 先直连服务，再经网关验证 |
| 审计数据为空 | 业务服务是否写入审计；数据库是否有记录 | 检查业务日志、数据库表和写入链路 |
| 查询很慢 | 表数据量、索引、时间范围是否合理 | 增加索引、限制查询范围或做归档 |
| Feign 调用失败 | 服务注册、调用方依赖、Nacos 是否正常 | 检查 `peach-monitor-openfeign-external` 和注册中心 |
| 运行信息泄露 | 接口是否未鉴权暴露 | 通过网关和服务侧权限控制保护监控接口 |
