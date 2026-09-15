# Peach Cloud Docker 环境启动手册

本文是 `deploy-pipline` 的主操作手册。目标是让第一次接触该目录的人能够明确知道：**先做什么、每条命令做什么、哪些命令会修改数据、怎样确认服务真的可用。**

> 适用环境：Windows + Docker Desktop。仓库中的自动化脚本是 POSIX Shell，建议在 Git Bash 或 WSL 中执行；`docker compose` 命令也可以直接在 PowerShell 中执行。

## 1. 先理解四个运行域

| 运行域 | Compose 文件 | 包含内容 | 是否由 Jenkins 启动 |
| --- | --- | --- | --- |
| DevOps | `compose/devops/docker-compose.yml` | GitLab、Jenkins、Nexus、Registry、Registry UI、Nginx | 否 |
| Middleware | `compose/middleware/docker-compose.yml` | MySQL、Redis、Nacos、MongoDB、RocketMQ、RocketMQ Dashboard | 否 |
| Observability | `compose/observability/docker-compose.yml` | Prometheus、Tempo、OTel Collector、Loki、Alloy、Grafana | 否 |
| Application | `compose/application/docker-compose.yml` | Peach Cloud 后端服务与前端 | 是 |

Jenkins 的职责只有：校验基础设施 → Maven 构建并发布到 Nexus → 构建 Docker 镜像 → 推送 Registry → 更新 Application。

## 2. 第一次启动前检查

从仓库根目录进入部署目录：

```bash
cd deploy-pipline
```

**作用：** 后续相对路径都以 `deploy-pipline` 为基准，避免在错误目录执行 Compose。

确认 Docker Desktop 已启动：

```bash
docker version
```

**作用：** 同时验证 Docker CLI 和 Docker daemon。只有 Client 信息、没有 Server 信息，通常表示 Docker Desktop 尚未正常启动。

确认 Compose V2 可用：

```bash
docker compose version
```

**作用：** 本项目使用 `docker compose`，不是旧版 `docker-compose`。

## 3. 创建私有环境文件

Git Bash / WSL：

```bash
cp env/deploy.env.example env/deploy.env
```

PowerShell：

```powershell
Copy-Item env/deploy.env.example env/deploy.env
```

**作用：** 复制模板生成本机私有配置。`env/deploy.env` 不应提交 Git。

至少修改：

- `PEACH_RUNTIME_ROOT`：Docker daemon 可访问的运行目录。
- `PEACH_LOG_ROOT`：统一日志目录。
- MySQL、Redis、MongoDB、Nexus、Grafana 密码。
- `PEACH_MONGO_URI`：使用 `MONGO_APP_USERNAME/MONGO_APP_PASSWORD` 的业务连接串。

Windows Docker Desktop 示例：

```dotenv
PEACH_RUNTIME_ROOT=/host_mnt/d/Coding/mine/new-mine/peach-cloud/deploy-pipline/runtime
PEACH_LOG_ROOT=/host_mnt/d/Coding/mine/new-mine/peach-cloud/deploy-pipline/runtime/logs
```

`/host_mnt/d/...` 是 Docker Desktop Linux VM 访问 Windows `D:\...` 的路径形式。

## 4. 推荐方式：一次完成基础设施 Bootstrap

从仓库根目录执行：

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/bootstrap.sh
```

这条命令不会启动 Peach Cloud 业务服务，它只准备 DevOps、Middleware 和 Observability。

Bootstrap 内部依次执行：

1. `inspect-existing.sh`：盘点现有容器、网络和 Volume，不删除任何资源。
2. `ensure-networks.sh`：缺少时创建 `peach-devops` 与 `peach-cloud-runtime` 网络。
3. `ensure-volumes.sh`：缺少时创建受保护 named volume；已有 Volume 原样复用。
4. `start.sh`：已有容器只执行 `docker start`，不存在时才由对应 Compose 创建。
5. `verify-infrastructure.sh`：验证 Registry、Nexus、中间件和跨网络连通性。
6. `init-mysql.sh`：执行 MySQL 幂等 SQL 初始化。
7. `ensure-mongodb-user.sh`：只确保 MongoDB 业务账号存在，**不创建 schema/index，不写 seed 数据**。
8. `init-nacos.sh`：幂等导入 Nacos namespace/config。
9. 再次执行基础设施验证。

### 单独执行某一步

只盘点现有环境：

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/inspect-existing.sh
```

只创建缺失网络：

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/ensure-networks.sh
```

只创建缺失 Volume：

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/ensure-volumes.sh
```

只启动基础设施：

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/start.sh
```

只做健康验证：

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/verify-infrastructure.sh
```

这些脚本都不会执行 `docker compose down -v`、`docker volume prune` 或删除已有数据库数据。

## 5. 不使用 Bootstrap，手动分组启动

先确保网络与 Volume 已创建：

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/ensure-networks.sh
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/ensure-volumes.sh
```

### 5.1 启动 DevOps

```bash
docker compose --env-file deploy-pipline/env/deploy.env \
  -f deploy-pipline/compose/devops/docker-compose.yml up -d
```

**作用：** 创建/启动 GitLab、Jenkins、Nexus、Registry、Registry UI、Nginx。`-d` 表示后台运行。

查看状态：

```bash
docker compose --env-file deploy-pipline/env/deploy.env \
  -f deploy-pipline/compose/devops/docker-compose.yml ps
```

### 5.2 启动 Middleware

```bash
docker compose --env-file deploy-pipline/env/deploy.env \
  -f deploy-pipline/compose/middleware/docker-compose.yml up -d
```

**作用：** 启动 MySQL、Redis、Nacos、MongoDB、RocketMQ NameServer、Broker 和 Dashboard。

验证：

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/verify-middleware.sh
```

该验证会执行 MySQL authenticated ping、Redis authenticated PING、Nacos health、MongoDB authenticated ping、RocketMQ Broker 注册检查，并实际请求 RocketMQ Dashboard 的 HTTP 首页确认可访问。

### 5.3 只创建 MongoDB 业务用户

```bash
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/init/ensure-mongodb-user.sh
```

**作用：** 如果业务用户不存在就创建，存在则保留。它不会执行任何 MongoDB 业务数据、集合、索引或 seed 初始化。

### 5.4 启动 Observability

```bash
docker compose --env-file deploy-pipline/env/deploy.env \
  -f deploy-pipline/compose/observability/docker-compose.yml up -d
```

**作用：** 启动 Prometheus、Tempo、OTel Collector、Loki、Alloy 与 Grafana。

## 6. RocketMQ 可视化控制台

中间件启动后，浏览器访问：

```text
http://localhost:18088
```

端口由 `ROCKETMQ_DASHBOARD_HOST_PORT` 控制，容器名为 `peach-rocketmq-dashboard`。Dashboard 通过 Docker 网络访问 `rocketmq-namesrv:9876`。

常用检查：

```bash
docker logs --tail 100 peach-rocketmq-dashboard
```

**作用：** 查看 Dashboard 最近 100 行启动日志。

```bash
docker exec peach-rocketmq-broker sh mqadmin clusterList -n rocketmq-namesrv:9876
```

**作用：** 直接确认 Broker 已经注册到 NameServer；Dashboard 无数据时优先执行它判断 RocketMQ 本身是否正常。

```bash
curl -fsS http://127.0.0.1:18088/ > /dev/null
```

**作用：** 从宿主机实际访问 Dashboard HTTP 服务；命令退出码为 `0` 表示 Web 入口可达。统一 `verify-middleware.sh` 已包含这项检查。

## 7. 常用访问地址

| 服务 | 默认地址 |
| --- | --- |
| Nacos | `http://localhost:8849/nacos/` |
| Nexus | `http://localhost:8081` |
| Registry API | `http://localhost:5000/v2/` |
| Registry UI | `http://localhost:5001` |
| RocketMQ Dashboard | `http://localhost:18088` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

GitLab/Jenkins 的对外访问方式以 `compose/devops/docker-compose.yml` 和本机 Nginx/hosts 配置为准。

## 8. 日常查看与安全停止

查看全部容器：

```bash
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
```

查看某个容器日志：

```bash
docker logs -f peach-nacos
```

`-f` 表示持续跟随日志，按 `Ctrl+C` 退出，不会停止容器。

安全停止 Middleware：

```bash
docker compose --env-file deploy-pipline/env/deploy.env \
  -f deploy-pipline/compose/middleware/docker-compose.yml down
```

**作用：** 停止并移除 Compose 创建的容器/默认网络引用，但 external named volume 不会被删除。

> **禁止使用 `down -v`。** `-v` 会请求删除 Compose Volume；本项目把数据 Volume 当作受保护资源，日常运维不允许带该参数。

如果只是临时停机并希望保留容器本身：

```bash
docker stop peach-mysql peach-redis peach-nacos peach-mongo peach-rocketmq-namesrv peach-rocketmq-broker peach-rocketmq-dashboard
```

恢复：

```bash
docker start peach-mysql peach-redis peach-nacos peach-mongo peach-rocketmq-namesrv peach-rocketmq-broker peach-rocketmq-dashboard
```

## 9. 启动业务服务

业务镜像已经存在于本地 Registry 后，可以手动执行：

```bash
PEACH_IMAGE_TAG=<git-sha> docker compose \
  --env-file deploy-pipline/env/deploy.env \
  -f deploy-pipline/compose/application/docker-compose.yml \
  pull
```

**作用：** 拉取指定 Git SHA 对应的业务镜像。

随后启动：

```bash
PEACH_IMAGE_TAG=<git-sha> docker compose \
  --env-file deploy-pipline/env/deploy.env \
  -f deploy-pipline/compose/application/docker-compose.yml \
  up -d
```

日常推荐仍然由 Jenkins 完成这两个动作，避免人工选择错误的镜像版本。

## 10. 启动失败时怎么查

1. `docker ps -a`：先确认容器是否退出。
2. `docker logs --tail 200 <container>`：看退出原因。
3. `docker network inspect peach-cloud-runtime`：确认中间件和业务容器是否在同一运行网络。
4. `docker volume inspect <volume>`：确认实际挂载的持久化 Volume。
5. `PEACH_ENV_FILE=... verify-infrastructure.sh`：重新执行统一基础设施验证。
6. Compose 报变量缺失时，检查 `env/deploy.env`，不要把真实密码写回示例文件。

更多网络、Volume、迁移和数据保护原则见 [architecture-and-operations.md](architecture-and-operations.md)。
