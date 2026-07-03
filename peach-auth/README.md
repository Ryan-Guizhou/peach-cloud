# peach-auth

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-auth`  
类型：认证与权限业务域聚合模块

## 模块定位

`peach-auth` 承载用户、角色、菜单、资源、路由权限、登录态和用户操作日志相关能力。它提供实体模型、领域服务、REST 接口、OpenFeign 外部接口、操作日志注解能力和可启动服务。

本模块解决：

- 认证域核心数据模型和服务分层。
- 登录、用户、角色、菜单、资源、路由等 REST 入口。
- 面向其他服务的角色、路由等外部查询接口。
- `@UserOperLog` 操作日志采集入口。

本模块不解决：

- 网关过滤、统一鉴权拦截和网关路由转发，相关逻辑位于 `peach-gateway` 与 `peach-satoken`。
- Sa-Token、Redis、数据库等基础设施部署。
- 生产密码策略、限流、风控、审计归档等完整安全治理。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-auth-common` | 认证域公共常量、工具和共享对象 |
| `peach-auth-entity` | DO、DTO、QO、VO 等认证域模型 |
| `peach-auth-service` | 用户、角色、菜单、资源、登录、权限等领域服务 |
| `peach-auth-rest` | 内部 REST 接口和外部 REST 适配入口 |
| `peach-auth-external/peach-auth-openfeign-external` | 面向其他模块的 OpenFeign 客户端 |
| `peach-auth-external/peach-auth-log-external` | 用户操作日志注解、枚举和外部能力 |
| `peach-auth-launch` | Spring Boot 启动模块 |

## 关键入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-auth-launch/src/main/java/com/peach/auth/launch/PeachAuthServiceApplication.java` |
| 配置文件 | `peach-auth-launch/src/main/resources/application-dev.yml` |
| REST 包 | `peach-auth-rest/src/main/java/com/peach/auth/rest` |
| 服务接口 | `peach-auth-service/src/main/java/com/peach/auth/service` |
| 实体模型 | `peach-auth-entity/src/main/java/com/peach/auth/entity` |
| OpenFeign | `peach-auth-external/peach-auth-openfeign-external/src/main/java/com/peach/auth/openfeign` |

## REST 能力

当前 REST 控制器覆盖以下入口：

| 控制器 | 路径前缀 | 说明 |
| --- | --- | --- |
| `LoginController` | `/auth` | 登录和认证相关入口 |
| `UserController` | `/auth` | 用户相关接口 |
| `RoleController` | `/auth/role` | 角色管理 |
| `MenuController` | `/auth/menu` | 菜单管理 |
| `ResourceController` | `/auth/resource` | 资源管理 |
| `RouterController` | `/auth/router` | 路由权限 |
| `FileController` | `/auth/file` | 认证域文件相关入口 |
| `RoleExternalController` | `/auth/external` | 外部角色查询接口 |
| `RouterExternalController` | `/auth/external` | 外部路由查询接口 |

## 运行机制

1. `peach-auth-launch` 启动服务并加载当前 profile 的配置。
2. REST 层接收登录、用户、角色、菜单、资源、路由等请求。
3. Service 层组织认证域业务逻辑，并通过 DAO 访问数据库。
4. 外部模块通过 `peach-auth-openfeign-external` 或网关路由访问认证能力。
5. 使用 `@UserOperLog` 的业务动作会进入操作日志链路，具体持久化依赖服务实现和数据库配置。

## 配置说明

- 启动配置位于 `peach-auth-launch/src/main/resources/application-*.yml`。
- 本地、Docker、生产环境分别使用 `dev`、`docker`、`prod` profile。
- 数据库、Redis、Nacos、Sa-Token 参数需要结合运行环境配置，不能把生产密钥写入仓库。
- 权限缓存和会话持久化依赖外部 Redis / Sa-Token 配置是否正确。

## 边界与限制

- 认证域提供权限数据和登录接口，但网关侧鉴权是否放行由网关和 Sa-Token 配置决定。
- 外部接口面向服务间调用，不应直接暴露为无鉴权公网接口。
- 操作日志只能记录进入注解切面的调用，无法覆盖绕过 Spring Bean 或未标注注解的行为。
- 登录失败重试、验证码、密码复杂度、账号锁定等生产安全策略需要按业务补齐。

## 构建与验证

```bash
mvn -f "peach-auth/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-auth/peach-auth-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-auth/peach-auth-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 登录接口不可用 | `peach-auth-launch` 是否启动；网关路由是否指向认证服务 | 先直连认证服务端口验证，再检查 `peach-gateway` 路由 |
| 权限数据为空 | 数据库脚本是否执行；用户、角色、资源关系是否存在 | 检查 `sql/` 初始化脚本和认证域表数据 |
| OpenFeign 调用失败 | 服务名、Nacos 注册、Feign 配置是否正确 | 确认认证服务已注册，调用方引入 `peach-auth-openfeign-external` |
| 操作日志未记录 | 是否使用 `@UserOperLog`；调用是否经过 Spring 代理 | 检查注解位置、AOP 生效条件和日志服务实现 |
| 会话或权限缓存异常 | Redis、Sa-Token、序列化配置是否一致 | 检查 Redis 连接和 `peach-satoken` 相关配置 |
