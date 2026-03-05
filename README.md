# peach-cloud

## 项目简介
`peach-cloud` 是一个基于 Spring Boot + Spring Cloud Alibaba 的多模块微服务项目，采用 Maven 聚合工程组织，核心能力包括网关、认证、配置、文件、消息、监控及中间件组件封装。

## 技术栈
- Java 8
- Spring Boot 2.7.13
- Spring Cloud 2021.0.5
- Spring Cloud Alibaba 2021.0.5.0
- Nacos / MySQL / Redis
- Maven 多模块

## 模块导航
- `peach-gateway`: API 网关入口
- `peach-auth`: 认证与权限域
- `peach-monitor`: 监控与审计域
- `peach-fileservice`: 文件服务域
- `peach-message`: 消息域
- `peach-setting`: 系统配置域
- `peach-common`: 公共基础能力
- `peach-component`: 通用组件库（captcha/email/threadpool/initialize/file）
- `peach-middleware`: 中间件封装（redis/redisson/mongo/kafka/rocket）
- `peach-sample`: 示例工程

每个模块及子模块目录下均提供 `README.md`，包含模块职责、已实现功能和未完成功能。

## 已实现功能
- 完成多模块拆分与统一依赖/插件管理，支持按模块独立构建。
- 完成 6 个核心服务启动模块（gateway/auth/monitor/fileservice/message/setting）。
- 完成容器化构建链路：通用 `Dockerfile` + `docker-compose.yml`。
- 完成本地依赖编排：`Nacos + MySQL + Redis` 一键拉起。

## 未完成功能
- 需要补齐模块边界契约文档（接口输入输出、错误码、事件契约）。
- 需要补齐 CI/CD 流程（单测、集成测试、镜像发布、质量门禁）。
- 需要补齐生产级观测能力（日志规范、Metrics、Tracing、告警规则）。
- 部分子模块仍以骨架为主，需继续补齐领域实现和测试覆盖。

## 本地构建
```bash
mvn clean package -DskipTests -Pdevelopment
```

## Docker 启动
已默认集成：
- MySQL: `3306`，`root/123456`
- Redis: `6379`，密码 `123456`
- Nacos: `8848`
- 业务服务: `18080-18085`

Windows:
```bat
bin\start.bat up
```

Linux/macOS:
```sh
sh bin/start.sh up
```

常用命令：
```bash
# 停止
sh bin/start.sh down
# 查看日志
sh bin/start.sh logs
# 查看状态
sh bin/start.sh ps
```

## 说明
- 当前服务配置通过 `application-dev.yml` + Nacos 外部配置协同生效。
- 若 Nacos 中配置与环境变量冲突，请以你在配置中心的优先级策略为准。
