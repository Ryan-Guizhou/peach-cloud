# peach-cloud

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
适用版本：JDK 8，Spring Boot 2.7.13，Spring Cloud 2021.0.5，Spring Cloud Alibaba 2021.0.5.0  
项目版本：`1.0.0-SNAPSHOT`，Maven 坐标组：`com.peach`

## 项目定位

`peach-cloud` 是一个基于 Maven 聚合工程组织的微服务项目。仓库中同时包含后端业务域、网关、公共组件、中间件 starter、示例工程、前端工程、SQL 初始化脚本和本地 Docker Compose 编排文件。

这个仓库主要解决以下问题：

- 统一管理 Spring Boot、Spring Cloud、Spring Cloud Alibaba、Sa-Token、MyBatis、PageHelper、Knife4j、Redis、Redisson、RocketMQ、对象存储等依赖版本。
- 按业务域拆分认证、文件、消息、系统配置、监控、代码生成等服务。
- 将可复用能力沉淀为 `peach-component` 和 `peach-middleware` 下的 starter / autoconfigure 模块。
- 提供 `*-launch` 启动模块，便于本地、Docker 和外部配置中心环境运行。
- 提供前端工程 `peach-cloud-front`，用于和后端服务联调。

这个仓库不直接承诺以下能力：

- 不替代生产级部署平台、CI/CD 流水线或运行时治理系统。
- 不保证本地 `docker-compose.yml` 可以直接作为生产编排使用。
- 不把本地配置中的账号、密码、端口视为生产默认值。
- 不保证所有 starter 在未配置外部依赖时都具备完整生产语义，例如消息、分布式锁、对象存储、邮件、缓存等能力仍依赖真实中间件和业务配置。

## 仓库结构

```text
peach-cloud
├── bin/                  # Docker Compose 启停脚本
├── doc/                  # 接入说明、组件手册和治理文档
├── sql/                  # 数据库初始化和业务表脚本
├── peach-auth/           # 认证、用户、角色、资源、登录、操作日志
├── peach-gateway/        # Spring Cloud Gateway 网关
├── peach-fileservice/    # 文件领域服务和文件接口
├── peach-message/        # 站内信、公告、待办、未读状态、WebSocket 推送
├── peach-setting/        # 字典、值集、通知、多语言消息等系统配置
├── peach-monitor/        # 监控、审计和运行时接口
├── peach-generator/      # 数据源、元数据、模板和代码生成
├── peach-common/         # 公共常量、响应、异常、工具和基础模型
├── peach-component/      # captcha、email、initialize、storage、threadpool
├── peach-middleware/     # redis、redission、mongo、openfeign、satoken、rocket 等封装
├── peach-sample/         # 组件和中间件使用示例
├── peach-cloud-front/    # Vue 3 + Vite 前端工程
├── docker-compose.yml    # 本地依赖和后端服务编排
└── pom.xml               # Maven 聚合根 POM
```

说明：

- `target/`、`.flattened-pom.xml`、日志目录和 IDE 配置不是源码结构的一部分。
- `peach-cloud-front` 不是根 `pom.xml` 的 Maven module，需要使用 npm 单独构建。
- 后端可启动服务通常位于各业务域的 `*-launch` 子模块中。

## 模块导航

| 模块 | 子模块 / 入口 | 职责 |
| --- | --- | --- |
| `peach-gateway` | `peach-gateway-core`，`peach-gateway-launch` | 网关启动、路由入口、网关侧认证和聚合能力 |
| `peach-auth` | `common`，`entity`，`service`，`rest`，`external`，`launch` | 用户、角色、菜单、资源、登录、权限和操作日志 |
| `peach-fileservice` | `common`，`entity`，`service`，`rest`，`openfeign-external`，`launch` | 文件业务、存储接入、REST 接口和 OpenFeign 外部接口 |
| `peach-message` | `common`，`entity`，`service`，`rest`，`openfeign-external`，`launch` | 站内消息、公告、待办、未读状态和推送 |
| `peach-setting` | `common`，`entity`，`service`，`rest`，`openfeign-external`，`launch` | 字典、值集、通知、多语言消息等配置能力 |
| `peach-monitor` | `common`，`entity`，`service`，`rest`，`openfeign-external`，`launch` | 监控、审计、运行时查询和监控接口 |
| `peach-generator` | `common`，`entity`，`service`，`rest`，`launch` | 数据源、表元数据、模板、预览和代码生成 |
| `peach-common` | 单模块 | 公共响应、异常、常量、基础模型和工具类 |
| `peach-component` | `peach-captcha`，`peach-email`，`peach-storage`，`peach-initialize`，`peach-threadpool` | 与业务无关的通用组件 starter |
| `peach-middleware` | `peach-kafka`，`peach-rocket`，`peach-redis`，`peach-redission`，`peach-mongo`，`peach-satoken`，`peach-openfeign` | 中间件接入、自动配置、starter 和示例 |
| `peach-sample` | `SampleApplication` | 组件和中间件能力的本地示例应用 |
| `peach-cloud-front` | `src/`，`vite.config.ts` | Vue 3 + Vite + TypeScript 前端 |

## 启动入口

| 服务 | 启动类 | 配置目录 |
| --- | --- | --- |
| 网关 | `com.peach.gateway.launch.PeachGatewayApplication` | `peach-gateway/peach-gateway-launch/src/main/resources` |
| 认证服务 | `com.peach.auth.launch.PeachAuthServiceApplication` | `peach-auth/peach-auth-launch/src/main/resources` |
| 文件服务 | `com.peach.fileservice.launch.PeachFileserviceApplication` | `peach-fileservice/peach-fileservice-launch/src/main/resources` |
| 消息服务 | `com.peach.message.launch.PeachMessageApplication` | `peach-message/peach-message-launch/src/main/resources` |
| 配置服务 | `com.peach.setting.launch.PeachSettingApplication` | `peach-setting/peach-setting-launch/src/main/resources` |
| 监控服务 | `com.peach.monitor.launch.PeachMonitorApplication` | `peach-monitor/peach-monitor-launch/src/main/resources` |
| 代码生成服务 | `com.peach.generator.launch.PeachGeneratorApplication` | `peach-generator/peach-generator-launch/src/main/resources` |
| 示例服务 | `com.peach.sample.SampleApplication` | `peach-sample/src/main/resources` |
| 存储示例 | `com.peach.example.PeachStoreExampleApplication` | `peach-component/peach-storage/peach-store-example/src/main/resources` |
| RocketMQ 示例 | `com.peach.rocket.example.PeachRocketExampleApplication` | `peach-middleware/peach-rocket/peach-rocket-example/src/main/resources` |

## 技术栈与版本

主要版本由根 `pom.xml` 统一声明：

| 类别 | 版本 |
| --- | --- |
| Java | `1.8` |
| Spring Boot | `2.7.13` |
| Spring Cloud | `2021.0.5` |
| Spring Cloud Alibaba | `2021.0.5.0` |
| Sa-Token | `1.37.0` |
| MyBatis Spring Boot Starter | `2.3.1` |
| PageHelper | `1.4.7` |
| Knife4j | `4.4.0` |
| Hutool | `5.8.20` |
| Fastjson | `2.0.21` |
| Redisson | `3.26.1` |
| RocketMQ Spring | `2.2.3` |
| RocketMQ Client | `5.3.2` |
| MinIO Java SDK | `8.5.12` |

构建配置要点：

- 根 POM 使用 `${revision}` 管理项目内模块版本。
- `development` profile 默认激活，另外提供 `production`、`docker`、`test` profile。
- `maven-compiler-plugin` 统一使用 Java 8，并开启 `parameters`。
- `flatten-maven-plugin` 会在构建过程中生成 `.flattened-pom.xml`，该文件属于构建产物，不应作为文档结构的一部分。

## 快速构建

完整构建但跳过测试：

```bash
mvn clean package -DskipTests -Pdevelopment
```

只做 Maven 模型和模块校验：

```bash
mvn clean validate -Pdevelopment
```

构建指定业务域及其依赖：

```bash
mvn -pl peach-auth -am clean package -DskipTests -Pdevelopment
mvn -pl peach-gateway -am clean package -DskipTests -Pdevelopment
```

构建指定启动模块及其依赖：

```bash
mvn -pl peach-auth/peach-auth-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-fileservice/peach-fileservice-launch -am clean package -DskipTests -Pdevelopment
```

构建组件或中间件模块：

```bash
mvn -pl peach-component/peach-threadpool -am clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-rocket -am clean package -DskipTests -Pdevelopment
```

## 本地依赖与 Docker Compose

仓库提供 `docker-compose.yml` 和 `bin/` 脚本，用于本地启动 MySQL、Redis、Nacos 和多个后端服务。

| 服务 | 容器名 | 本地端口 |
| --- | --- | --- |
| MySQL | `peach-mysql` | `3307 -> 3306` |
| Redis | `peach-redis` | `6380 -> 6379` |
| Nacos | `peach-nacos` | `8849 -> 8848`，`9849 -> 9848` |
| Gateway | `peach-gateway` | `18080` |
| Auth | `peach-auth` | `18081` |
| Monitor | `peach-monitor` | `18082` |
| Fileservice | `peach-fileservice` | `18083` |
| Message | `peach-message` | `18084` |
| Setting | `peach-setting` | `18085` |

Windows：

```bat
bin\start.bat up
bin\start.bat ps
bin\start.bat logs
bin\start.bat down
```

Linux / macOS：

```sh
sh bin/start.sh up
sh bin/start.sh ps
sh bin/start.sh logs
sh bin/start.sh down
```

脚本支持的动作：

| 动作 | 说明 |
| --- | --- |
| `up` | 执行 `docker compose up -d --build` |
| `down` | 停止并移除 compose 服务 |
| `restart` | 先 `down` 再 `up` |
| `logs` | 跟随输出最近 200 行日志 |
| `ps` | 查看服务状态 |
| `build` | 只执行 compose build |

Windows 脚本可以通过第二个参数指定 compose 文件：

```bat
bin\start.bat up docker-compose.yml
```

Linux / macOS 脚本可以通过环境变量指定 compose 文件：

```sh
COMPOSE_FILE=docker-compose.yml sh bin/start.sh up
```

## 单服务本地运行

如果只想运行某个服务，可以先启动基础依赖，再用 Maven 启动对应 `*-launch` 模块。

示例：

```bash
mvn -pl peach-gateway/peach-gateway-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
mvn -pl peach-auth/peach-auth-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
mvn -pl peach-message/peach-message-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

注意事项：

- `application-dev.yml`、`application-docker.yml`、`application-prod.yml` 分别面向本地、Docker 和生产环境。
- 部分服务依赖 Nacos 外部配置，单独启动前需要确认 Nacos、数据库、Redis 等地址和账号与当前 profile 匹配。
- 不要把生产密钥、生产数据库地址、生产对象存储密钥提交到仓库。

## 数据库脚本

SQL 脚本位于 `sql/`：

| 文件 | 用途 |
| --- | --- |
| `init.sql` | 初始化入口脚本 |
| `PEACH_USER.sql`，`PEACH_ROLE.sql`，`PEACH_MENU.sql`，`PEACH_RESOURCE.sql` | 用户、角色、菜单和资源相关表 |
| `PEACH_AUTH_*.sql`，`USER_OPER_LOG.sql` | 认证、权限和操作日志相关表 |
| `PEACH_APPLICATION.sql`，`PEACH_ROUTER.sql`，`PEACH_FUNCTION.sql` | 应用、路由和功能配置 |
| `PEACH_GENERATOR.sql` | 代码生成模块相关表 |

执行前应确认：

- 当前数据库字符集、排序规则和连接用户权限符合项目要求。
- 脚本是否会覆盖已有表或数据。
- 本地 Docker Compose 使用的 MySQL 端口是 `3307`，不是默认 `3306`。

## 前端工程

前端位于 `peach-cloud-front/`，使用 Vue 3、Vite、TypeScript、Pinia、Vue Router、Ant Design Vue 和 Axios。

安装依赖：

```bash
cd peach-cloud-front
npm install
```

本地开发：

```bash
npm run dev
```

构建：

```bash
npm run build
```

预览构建产物：

```bash
npm run preview
```

前端工程说明：

- `package-lock.json` 已存在，建议使用 npm 保持锁文件一致。
- 前端 API 地址、代理和认证联调需要结合 `vite.config.ts` 与后端网关配置确认。
- 前端不是 Maven reactor 的一部分，根目录 Maven 构建不会自动执行前端构建。

## 配置约定

后端配置主要由三类来源共同决定：

1. 启动模块内的 `application.yml` 或 `application-*.yml`。
2. Nacos 等外部配置中心。
3. Docker Compose 或运行环境注入的环境变量。

常见 profile：

| Profile | 使用场景 |
| --- | --- |
| `dev` | 本地开发和直连本地依赖 |
| `docker` | Docker Compose 容器网络内运行 |
| `prod` | 生产或类生产环境 |
| `test` | 测试环境 |

配置修改建议：

- 优先在对应 `*-launch` 模块中查找当前服务的 `application-*.yml`。
- 如果服务依赖 Nacos，先确认本地 Nacos 已启动且命名空间、分组、配置文件名正确。
- 中间件 starter 的配置项应以各模块 README 和配置类为准，不要只根据根 README 猜测。

## 组件与中间件边界

`peach-component` 主要沉淀通用组件：

- `peach-captcha`：验证码能力。
- `peach-email`：邮件发送、模板、重试、路由等能力。
- `peach-storage`：对象存储和本地/云厂商 provider 封装。
- `peach-initialize`：初始化执行能力。
- `peach-threadpool`：线程池、异步执行和上下文传递能力。

`peach-middleware` 主要沉淀中间件接入：

- `peach-redis`：Redis 工具、多级缓存、Stream 等能力。
- `peach-redission`：Redisson 分布式锁、延迟队列、布隆过滤器、防重复等能力。
- `peach-rocket`：RocketMQ 生产、消费、事件建模、事务消息和示例。
- `peach-mongo`：Mongo 自动配置和 starter。
- `peach-satoken`：Web 与 Gateway 场景的 Sa-Token 封装。
- `peach-openfeign`：OpenFeign 自动配置和 starter。
- `peach-kafka`：Kafka 相关模块。

这些模块的 README 应明确：

- starter 提供什么 Bean、注解、模板类或 SPI。
- autoconfigure 的启用条件和默认实现。
- 业务如何接入、如何覆盖默认 Bean、如何扩展 provider / handler。
- 生产边界，例如幂等语义、事务语义、自动创建资源、路径安全、批量删除、队列阻塞、线程池拒绝策略等。

## 文档约定

- 中文文档使用 `README.md`。
- 英文文档使用 `README.en-US.md`。
- 模块 README 应优先描述当前模块真实存在的类、配置项、命令和限制。
- 不应把构建产物、IDE 目录、日志目录写入源码结构。
- 不应在 README 中写入真实生产地址、密钥、token、签名 URL 或账号密码。
- 当模块同时包含 `starter`、`autoconfigure`、`example` 时，文档要区分各 artifact 的职责。

## 验证建议

常规后端验证：

```bash
mvn clean validate -Pdevelopment
mvn clean package -DskipTests -Pdevelopment
```

指定模块验证：

```bash
mvn -pl peach-component/peach-storage -am clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-rocket -am clean package -DskipTests -Pdevelopment
```

前端验证：

```bash
cd peach-cloud-front
npm run build
```

Docker Compose 验证：

```bash
sh bin/start.sh build
sh bin/start.sh up
sh bin/start.sh ps
sh bin/start.sh logs
```

Windows 环境使用：

```bat
bin\start.bat build
bin\start.bat up
bin\start.bat ps
bin\start.bat logs
```

## 常见排障

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| Maven 找不到项目内模块版本 | 是否从仓库根目录执行；是否使用了 `${revision}`；是否缺少 `-am` | 在根目录执行命令，指定模块时加 `-am`，必要时先执行 `mvn clean install -DskipTests` |
| 构建生成 `.flattened-pom.xml` | 根 POM 启用了 `flatten-maven-plugin` | 这是构建产物，不要写入源码结构或人工维护 |
| 服务启动后读不到配置 | profile 是否正确；Nacos 是否启动；配置文件名、命名空间、分组是否匹配 | 确认 `application-*.yml` 和 Nacos 配置，检查启动参数 `spring.profiles.active` |
| Docker Compose 端口冲突 | 本地是否已有 MySQL、Redis、Nacos 或服务占用端口 | 修改 compose 端口映射，或停止本地占用进程 |
| 服务之间无法访问 | 是否在同一个 compose 网络；服务名是否使用容器内 DNS 名称 | Docker 内部访问优先使用服务名，不使用宿主机 localhost |
| 数据库连接失败 | MySQL 端口是否为 `3307`；账号密码是否和配置一致；脚本是否已执行 | 检查 compose 环境变量、连接串和 `sql/` 初始化脚本 |
| Redis 连接失败 | 本地端口是否为 `6380`；密码是否为当前配置要求 | 核对 `docker-compose.yml` 与服务配置中的 Redis 地址和密码 |
| Nacos 访问失败 | 端口是否为 `8849`；Nacos 容器是否健康 | 执行 `bin/start.* ps` 和 `bin/start.* logs` 查看状态 |
| 前端接口 404 或跨域 | 网关是否启动；前端代理是否指向正确端口；后端路由是否注册 | 检查 `vite.config.ts`、网关端口 `18080` 和后端服务状态 |
| starter Bean 未注入 | 是否引入了 `*-starter`；自动配置条件是否满足；配置项是否开启 | 查看对应模块 README、autoconfigure 模块和 Spring Boot 条件报告 |

## 维护建议

当新增或调整模块时，同步检查以下位置：

- 根 `pom.xml` 的 `<modules>` 和 `<dependencyManagement>`。
- 业务域 `pom.xml` 的子模块列表。
- `docker-compose.yml` 是否需要新增服务、端口、环境变量或依赖关系。
- `sql/` 是否需要新增初始化脚本。
- 模块 `README.md` 和 `README.en-US.md` 是否需要同步更新。
- 顶层 README 的模块导航和启动入口是否仍然准确。
