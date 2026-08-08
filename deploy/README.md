# Peach Cloud Docker Compose 部署

本目录提供 Peach Cloud 的 Docker Compose 一键部署方案，覆盖基础依赖、业务服务、Nacos 配置回填、Nginx 统一反代和常见运维操作。

如果只需要通过 `java -jar` 和 `nohup` 管理业务服务进程，不希望混用 Docker Compose，请使用独立方案：[jar/README.md](jar/README.md)。该方案只规划 jar 启停、PID 和日志目录，不负责中间件安装和数据初始化。

## 适用范围

适用于单机 Docker Compose 部署：

- MySQL 8.0
- Redis 7.2
- Nacos 2.3.2
- peach-gateway、peach-auth、peach-monitor、peach-fileservice、peach-message、peach-setting、peach-generator
- Nginx 反代前端、API 和 WebSocket

不解决多节点高可用、Kubernetes 编排、生产证书自动签发、数据库备份恢复策略和灰度发布。这些能力应在生产部署规范中单独设计。

## 目录结构

```text
deploy/
  docker-compose.yml
  .env.example
  peach.sh
  docker/Dockerfile.service
  nacos/
    config/*.yml
    import-nacos.sh
  nginx/
    conf.d/peach.conf
    html/.gitkeep
  runtime/
    data/
    logs/
    upload/
    config/
    nginx/html/
    nacos/nacos.env
```

`runtime/` 是运行期目录，默认不提交 Git：

- `runtime/data/mysql`：MySQL 数据目录。
- `runtime/data/redis`：Redis AOF 数据目录。
- `runtime/data/nacos`：Nacos 数据目录。
- `runtime/upload/peach-fileservice`：文件服务本地上传目录。
- `runtime/logs/*`：容器日志挂载目录。
- `runtime/nginx/html`：前端构建产物发布目录。
- `runtime/nacos/nacos.env`：Nacos namespace 运行期确认文件。

`deploy/docker-compose.yml` 中的挂载路径均使用相对路径；`peach.sh` 会先切换到 `deploy/` 目录再执行 Docker Compose，以便仓库移动到其他目录后仍可使用同一套部署文件。

## 快速部署

进入部署目录：

```bash
cd deploy
```

初始化部署目录：

```bash
./peach.sh init
```

编辑 `.env`，至少修改：

```env
MYSQL_ROOT_PASSWORD=change_me_mysql_root_password
MYSQL_HOST=mysql:3306
REDIS_PASSWORD=change_me_redis_password
REDIS_HOST=redis:6379
NACOS_NAMESPACE_ID=peach-cloud
NACOS_NAMESPACE_NAME=peach-cloud
NACOS_GROUP=PEACH-CLOUD
```

如需让构建出的业务镜像携带维护人、团队、源码和版本信息，可继续修改：

```env
IMAGE_AUTHORS="Mr Shu"
IMAGE_VENDOR=Peach Cloud
```

这些值会写入业务镜像的 OCI labels，不参与应用运行，不应填写密码、token 或个人隐私信息。

构建后端镜像：

```bash
./peach.sh build
```

如需同时部署前端：

```bash
./peach.sh front:build
```

启动全部服务：

```bash
./peach.sh up
```

查看状态：

```bash
./peach.sh ps
./peach.sh health
```

如果 `./peach.sh up` 中途失败，不要立即执行 `clean:data`。先执行：

```bash
./peach.sh ps
./peach.sh logs
./peach.sh health
```

根据失败阶段选择补救命令。只有确认 MySQL、Redis、Nacos 的运行期数据可以丢弃时，才使用 `./peach.sh clean:data`。

## Nacos namespace 与配置一致性

部署脚本使用 `.env` 中的 `NACOS_NAMESPACE_ID` 作为唯一 namespace 标识：

```env
NACOS_NAMESPACE_ID=peach-cloud
NACOS_NAMESPACE_NAME=peach-cloud
NACOS_GROUP=PEACH-CLOUD
```

`./peach.sh up` 会执行以下流程：

1. 启动 Nacos。
2. 检查 namespace 是否存在。
3. 不存在时用 `NACOS_NAMESPACE_ID` 创建 namespace。
4. 将 `deploy/nacos/config/*.yml` 导入同一个 namespace 和 group。
5. 将业务容器的 Nacos Config 和 Discovery namespace 设置为同一个 `NACOS_NAMESPACE_ID`。
6. 写入 `runtime/nacos/nacos.env` 作为运行期确认文件。

因此配置写入、服务读取配置、服务注册发现使用同一个 namespace。

手动重新导入配置：

```bash
./peach.sh nacos:import
```

如需对已经存在的 MySQL 数据目录补导初始化种子数据，可执行：

```bash
./peach.sh mysql:init
```

`sql/INIT.sql` 使用 `INSERT IGNORE`，用于补齐缺失的初始化数据；不会自动删除或重建已有业务数据。
导入命令和 SQL 文件均显式使用 `utf8mb4`，用于避免中文初始化数据进入 MySQL 后乱码。

## MySQL 大小写不敏感

MySQL 服务启动参数固定包含：

```text
--lower-case-table-names=1
```

该参数用于保证表名大小写不敏感。注意：

- 该参数必须在 MySQL 数据目录首次初始化前设置。
- 如果 `runtime/data/mysql` 已经初始化，再修改该参数通常不会生效。
- `./peach.sh up` 会检查 `lower_case_table_names`，不是 `1` 会直接失败。
- 脚本不会自动删除 MySQL 数据目录。

如需重新初始化数据库，必须明确执行：

```bash
./peach.sh clean:data
```

该命令会二次确认，并删除运行期数据目录。

## Nginx 反代

Nginx 是默认对外入口：

```text
/             -> runtime/nginx/html 前端静态文件
/api/         -> peach-gateway:18080
/webSocket/   -> peach-gateway:18080
/nacos/       -> nacos:8848
```

配置文件：

```text
deploy/nginx/conf.d/peach.conf
```

重新加载 Nginx：

```bash
./peach.sh nginx:reload
```

生产环境如果暴露 `/nacos/`，应通过网络访问控制、Nginx 鉴权或移除该 location 进行限制。

## 常用运维命令

| 命令 | 含义 | 是否会删除数据 | 常见使用时机 |
| --- | --- | --- | --- |
| `./peach.sh init` | 创建 `deploy/.env` 和 `runtime/` 目录 | 否 | 首次部署前 |
| `./peach.sh build` | Maven 打包后端 jar，并构建业务服务镜像 | 否 | Java 代码或镜像 label 变更后 |
| `./peach.sh front:build` | 构建前端并发布到 `runtime/nginx/html` | 会覆盖前端静态产物，不删数据库 | 前端代码变更后 |
| `./peach.sh up` | 启动基础容器、补导 MySQL 种子数据、导入 Nacos 配置、启动业务服务和 Nginx | 否 | 日常启动或配置修复后 |
| `./peach.sh down` | 停止容器但保留 `runtime/data` | 否 | 临时停机 |
| `./peach.sh restart` | 重启所有容器 | 否 | 配置导入后需要整体刷新 |
| `./peach.sh restart peach-auth` | 只重启指定 compose 服务 | 否 | 单个服务异常或配置刷新 |
| `./peach.sh logs` | 跟随查看所有容器最近日志 | 否 | 不知道失败点时 |
| `./peach.sh logs peach-gateway` | 跟随查看指定服务日志 | 否 | 已定位到某个服务时 |
| `./peach.sh ps` | 查看容器状态、端口和健康状态 | 否 | 任意失败后的第一步 |
| `./peach.sh health` | 检查 Compose 状态、MySQL 大小写、MySQL 种子数据、Nacos 配置导入 | 否 | 启动后验收 |
| `./peach.sh mysql:init` | 对已有 MySQL 数据目录补导 `sql/INIT.sql` 种子数据 | 否，使用 `INSERT IGNORE` | 表已建好但初始化菜单、用户或租户缺失 |
| `./peach.sh nacos:import` | 将 `deploy/nacos/config/*.yml` 渲染后导入 Nacos | 否，会覆盖同名 Nacos 配置 | 修改 Nacos 配置模板或 `.env` 后 |
| `./peach.sh nginx:reload` | 让运行中的 Nginx 重新加载配置 | 否 | 修改 `nginx/conf.d/peach.conf` 后 |
| `./peach.sh clean:logs` | 删除 `runtime/logs` 下日志文件 | 不删业务数据 | 日志过大 |
| `./peach.sh clean:data` | 删除 `runtime/data` 和 `runtime/upload` | 是，会删除 MySQL、Redis、Nacos 和上传文件数据 | 只在确认要全量重建本地环境时使用 |

命令设计原则：

- 能补救的问题优先补救，例如 `mysql:init`、`nacos:import`、`restart <service>`。
- `clean:data` 是重建环境命令，不是普通排障命令。
- 配置变更优先执行 `nacos:import`，再重启受影响服务，不必每次全量删除。

## 配置文件

Nacos 配置模板位于：

```text
deploy/nacos/config/
```

当前提供：

- `peach-datasource.yml`
- `peach-redis.yml`
- `peach-openfeign.yml`
- `peach-satoken.yml`
- `peach-store.yml`
- `peach-gateway.yml`
- `peach-auth.yml`
- `peach-monitor.yml`
- `peach-fileservice.yml`
- `peach-message.yml`
- `peach-setting.yml`
- `peach-generator.yml`

模板中的数据库和 Redis 密码会在导入时从 `.env` 回填，不应把真实生产密码提交到 Git。

## 业务镜像元信息

业务服务共用 `deploy/docker/Dockerfile.service` 构建镜像。构建时会写入 OCI 标准 labels，便于后续在 Docker、镜像仓库或 CI/CD 中追踪责任人和版本来源。

| Label | 来源 | 含义 |
| --- | --- | --- |
| `org.opencontainers.image.title` | `docker-compose.yml` 中每个服务单独设置 | 镜像标题，例如 `Peach Auth` |
| `org.opencontainers.image.description` | `docker-compose.yml` 中每个服务单独设置 | 服务说明 |
| `org.opencontainers.image.authors` | `.env` 的 `IMAGE_AUTHORS` | 维护人或团队 |
| `org.opencontainers.image.vendor` | `.env` 的 `IMAGE_VENDOR` | 组织或产品方 |
| `org.opencontainers.image.version` | `.env` 的 `PEACH_IMAGE_TAG` | 镜像版本 |

构建后可以检查镜像 label：

```bash
docker image inspect peach-cloud/peach-auth:${PEACH_IMAGE_TAG:-latest}
```

如果镜像元信息不正确，只需要修改 `.env` 后重新执行：

```bash
./peach.sh build
```

不需要删除 `runtime/data`。

## 排障

### 失败后先判断处于哪个阶段

`./peach.sh up` 的阶段顺序如下：

```text
1. 创建目录和读取 .env
2. 启动 mysql、redis、nacos
3. 等待基础容器 healthy
4. 检查 MySQL lower_case_table_names
5. 检查并补导 INIT.sql 种子数据
6. 导入 Nacos 配置
7. 启动 peach-* 业务服务和 nginx
```

失败后按阶段处理：

| 失败阶段 | 典型现象 | 下一步 | 是否需要全删重来 |
| --- | --- | --- | --- |
| 读取 `.env` | 提示 `.env` 缺失或变量为空 | 执行 `./peach.sh init`，编辑 `.env` 后重跑 | 不需要 |
| 拉镜像或构建镜像 | 下载很慢、镜像构建失败 | 单独执行 `docker compose pull` 或 `./peach.sh build` 看具体镜像/模块 | 不需要 |
| 镜像 label 不正确 | `docker image inspect` 中 authors、vendor、version 不符合预期 | 修改 `.env` 中 `IMAGE_AUTHORS`、`IMAGE_VENDOR` 或 `PEACH_IMAGE_TAG`，重新执行 `./peach.sh build` | 不需要 |
| 端口绑定 | `Ports are not available` | 修改 `.env` 中对应 `*_HOST_PORT`，或停止占用端口的本机进程 | 不需要 |
| MySQL 已启动但认证失败 | `Access denied for user 'root'` | 说明 `runtime/data/mysql` 已有旧密码；改回旧密码，或确认可丢数据后 `clean:data` | 只有忘记旧密码且数据可丢时才需要 |
| MySQL 表存在但初始化数据缺失 | 菜单、租户、用户为空 | 执行 `./peach.sh mysql:init` | 不需要 |
| Nacos 配置未导入 | namespace 存在但配置数为 0，业务读不到配置 | 执行 `./peach.sh nacos:import`，再 `./peach.sh restart <service>` | 不需要 |
| 业务服务启动失败 | 基础容器 healthy，但 `peach-*` 退出 | 执行 `./peach.sh logs <service>`，优先检查 Nacos、MySQL、Redis 地址和密码 | 不需要 |
| Nginx 页面异常 | `/`、`/api/`、`/doc.html` 访问异常 | 执行 `./peach.sh logs nginx`，修改 `peach.conf` 后 `./peach.sh nginx:reload` | 不需要 |

只有下面两种情况建议重建本地数据：

- MySQL 初始化时 `lower_case_table_names` 不是 `1`，且当前数据可以丢弃。
- 忘记当前 `runtime/data/mysql` 的 root 密码，且当前数据库内容可以丢弃。

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| MySQL 启动后业务表异常 | `lower_case_table_names` 是否为 `1` | 如果不是 `1`，需要换空数据目录重新初始化 |
| 服务读取不到 Nacos 配置 | `runtime/nacos/nacos.env` 与容器环境变量是否一致 | 执行 `./peach.sh nacos:import` 后重启服务 |
| MySQL 初始化数据不完整 | `PEACH_TENANT`、`PEACH_APPLICATION`、`PEACH_USER`、`PEACH_MENU` 是否都有数据 | 执行 `./peach.sh mysql:init` 补导种子数据 |
| Nacos namespace 存在但配置数为 0 | Git Bash/Windows curl 路径兼容或导入脚本中断 | 执行 `./peach.sh nacos:import`，再执行 `./peach.sh health` |
| Nginx 返回前端 404 | `runtime/nginx/html` 是否有前端构建产物 | 执行 `./peach.sh front:build` |
| `/api/` 不通 | `peach-gateway` 是否 healthy/running | 执行 `./peach.sh logs peach-gateway` |
| WebSocket 断开 | Nginx `/webSocket/` 代理头是否保留 | 检查 `nginx/conf.d/peach.conf` |
| Docker 镜像构建失败 | 后端 jar 是否已由 Maven 打包生成 | 执行 `./peach.sh build` 查看 Maven 阶段输出 |

## 系统视角优化建议

当前部署方案适合单机演示、联调和小规模测试。站在整个系统角度，后续可以按优先级优化：

1. 密钥治理：将 `.env` 从人工维护升级为外部密钥系统或 CI/CD 注入，避免环境漂移。
2. 健康检查：为每个 `peach-*` 服务补充 HTTP 健康检查，避免只看容器 running。
3. 备份恢复：增加 MySQL、Nacos 配置、Redis 数据和上传文件的备份/恢复脚本。
4. 可观测性：统一业务日志、Nginx 日志和容器状态采集，保留最近失败上下文。
5. 配置一致性：增加脚本校验 Nacos namespace、group、配置数量和业务 `application-docker.yml` 导入项。
6. 端口预检查：`up` 前检测 `.env` 中主机端口是否被占用，提前给出明确提示。
7. 镜像版本治理：生产环境不要长期使用 `PEACH_IMAGE_TAG=latest`，应使用可追溯版本号。
8. 数据迁移：初始化 SQL 只负责种子数据；真实版本演进建议引入 Flyway 或 Liquibase。

## 验证命令

部署文件修改后建议执行：

```bash
node ../scripts/check-utf8.mjs
git diff --check
docker compose --env-file .env -f docker-compose.yml config
```

其中 `docker compose config` 需要先准备 `deploy/.env`。
