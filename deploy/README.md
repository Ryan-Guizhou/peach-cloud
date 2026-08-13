# Peach Cloud Docker Desktop 部署

本目录提供本地 Docker Desktop 的单机 Docker Compose 部署方案，覆盖 MySQL、Redis、Nacos、7 个后端服务镜像、前端 Nginx 镜像、Nacos 配置导入和常见运维命令。

CI/CD 自动构建部署方案在 `deploy-pipline/`，两套方案的运行结构一致，区别是：

| 方案 | 镜像来源 | 适用场景 |
| --- | --- | --- |
| `deploy/` | 本机执行 `./peach.sh build` 和 `./peach.sh front:build` 构建镜像 | 本地联调、演示、单机测试 |
| `deploy-pipline/` | Jenkins 构建镜像并推送到 Registry，再由 Compose 拉取 | GitLab Webhook 自动部署 |

## 目录结构

```text
deploy/
  docker-compose.yml
  .env.example
  peach.sh
  docker/
    Dockerfile.service
    Dockerfile.front
  nacos/
    config/*.yml
    config/*.json
    import-nacos.sh
  nginx/
    peach.conf.template
    conf.d/peach.conf
  runtime/
    build/front-dist/
    data/
    logs/
    upload/
    config/
    nacos/nacos.env
```

`runtime/` 是本地运行期目录，不应提交 Git。重要目录：

| 目录 | 用途 |
| --- | --- |
| `runtime/build/front-dist` | `front:build` 临时放置前端 dist，用于构建 `peach-front` 镜像 |
| `runtime/data/mysql` | MySQL 数据目录 |
| `runtime/data/redis` | Redis 数据目录 |
| `runtime/data/nacos` | Nacos 数据目录 |
| `runtime/upload/peach-fileservice` | 文件服务本地上传目录 |
| `runtime/logs/*` | 容器日志挂载目录 |

## 快速部署

进入部署目录：

```bash
cd deploy
```

初始化：

```bash
./peach.sh init
```

编辑 `.env`，至少修改：

```env
MYSQL_ROOT_PASSWORD=change_me_mysql_root_password
REDIS_PASSWORD=change_me_redis_password
OSS_ACCESS_KEY=change_me_oss_access_key
OSS_SECRET_KEY=change_me_oss_secret_key
COS_ACCESS_KEY=change_me_cos_access_key
COS_SECRET_KEY=change_me_cos_secret_key
NACOS_NAMESPACE_ID=peach-cloud
NACOS_NAMESPACE_NAME=peach-cloud
NACOS_GROUP=PEACH-CLOUD
```

构建后端镜像：

```bash
./peach.sh build
```

构建前端镜像：

```bash
./peach.sh front:build
```

启动全部服务：

```bash
./peach.sh up
```

验收：

```bash
./peach.sh ps
./peach.sh health
curl http://localhost:${NGINX_HTTP_PORT:-80}/
```

## 镜像

本地方案会构建 8 个镜像：

```text
peach-cloud/peach-gateway:${PEACH_IMAGE_TAG:-latest}
peach-cloud/peach-auth:${PEACH_IMAGE_TAG:-latest}
peach-cloud/peach-monitor:${PEACH_IMAGE_TAG:-latest}
peach-cloud/peach-fileservice:${PEACH_IMAGE_TAG:-latest}
peach-cloud/peach-message:${PEACH_IMAGE_TAG:-latest}
peach-cloud/peach-setting:${PEACH_IMAGE_TAG:-latest}
peach-cloud/peach-generator:${PEACH_IMAGE_TAG:-latest}
peach-cloud/peach-front:${PEACH_IMAGE_TAG:-latest}
```

后端服务共用 `deploy/docker/Dockerfile.service`。前端镜像使用 `deploy/docker/Dockerfile.front`，基于 `nginx:1.25-alpine`，把前端 dist 和 Nginx 模板打进镜像。

镜像标签由 `.env` 的 `PEACH_IMAGE_TAG` 控制。本地调试可以继续使用 `latest`；需要保留多版本时，改成明确版本号后重新执行 `build` 和 `front:build`。

## 端口与 Nginx

`.env` 中主机端口：

| 变量 | 默认值 | 含义 |
| --- | --- | --- |
| `NGINX_HTTP_PORT` | `80` | 宿主机访问前端和 API 的入口端口 |
| `MYSQL_HOST_PORT` | `3307` | 宿主机访问 MySQL 的端口 |
| `REDIS_HOST_PORT` | `6380` | 宿主机访问 Redis 的端口 |
| `NACOS_HOST_PORT` | `8849` | 宿主机访问 Nacos HTTP 的端口 |

服务内部端口：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `GATEWAY_PORT` | `18080` | `peach-gateway` 容器监听端口，也会注入前端 Nginx 模板 |
| `AUTH_PORT` | `18081` | `peach-auth` 容器监听端口 |
| `MONITOR_PORT` | `18082` | `peach-monitor` 容器监听端口 |
| `FILESERVICE_PORT` | `18083` | `peach-fileservice` 容器监听端口 |
| `MESSAGE_PORT` | `18084` | `peach-message` 容器监听端口 |
| `SETTING_PORT` | `18085` | `peach-setting` 容器监听端口 |
| `GENERATOR_PORT` | `18086` | `peach-generator` 容器监听端口 |

访问链路：

```text
http://localhost:${NGINX_HTTP_PORT}
  -> peach-front:80
  -> /api 代理到 peach-gateway:${GATEWAY_PORT}
```

Nginx 模板是 `deploy/nginx/peach.conf.template`。容器启动时由 Nginx 官方镜像渲染 `${GATEWAY_PORT}`，所以你修改 `GATEWAY_PORT` 后，需要重新启动 `peach-front`：

```bash
./peach.sh restart peach-front
```

当前本地方案没有配置 HTTPS。需要 HTTPS 时，单独补 443 server block、证书挂载和端口映射。

## Nacos 与初始化数据

`./peach.sh up` 的启动顺序：

```text
1. 读取 .env，创建 runtime 目录
2. 启动 mysql、redis、nacos
3. 等待基础容器 healthy
4. 检查 MySQL lower_case_table_names
5. 检查并补导 sql/INIT.sql 种子数据
6. 导入 `deploy/nacos/config/*.yml`、`*.yaml` 和 `*.json`
7. 启动 7 个后端服务和 peach-front
```

Nacos 配置使用 `.env` 中的：

```env
NACOS_NAMESPACE_ID=peach-cloud
NACOS_NAMESPACE_NAME=peach-cloud
NACOS_GROUP=PEACH-CLOUD
```

重新导入 Nacos 配置：

```bash
./peach.sh nacos:import
```

`peach-openfeign.yml` 中启用了 Sentinel Nacos 数据源，脚本会同时导入以下两个 JSON 配置：

```text
peach-openfeign-sentinel-flow-rules.json
peach-openfeign-sentinel-degrade-rules.json
```

这两个 Data ID 必须和 `peach-openfeign.yml` 中的 `spring.cloud.sentinel.datasource.*.nacos.data-id` 保持一致，否则服务启动后无法从 Nacos 拉取 Feign 限流和熔断规则。

补导 MySQL 种子数据：

```bash
./peach.sh mysql:init
```

`sql/INIT.sql` 使用 `INSERT IGNORE`，不会删除已有业务数据。

## 常用命令

| 命令 | 含义 | 是否删除数据 |
| --- | --- | --- |
| `./peach.sh init` | 创建 `.env` 和运行期目录 | 否 |
| `./peach.sh build` | Maven 打包并构建 7 个后端镜像 | 否 |
| `./peach.sh front:build` | 构建前端 dist 并构建 `peach-front` 镜像 | 否 |
| `./peach.sh up` | 启动基础服务、导入配置、启动后端和前端 | 否 |
| `./peach.sh down` | 停止容器，保留数据 | 否 |
| `./peach.sh restart [svc]` | 重启全部或指定服务 | 否 |
| `./peach.sh logs [svc]` | 查看全部或指定服务日志 | 否 |
| `./peach.sh ps` | 查看容器状态 | 否 |
| `./peach.sh health` | 检查 Compose、MySQL 和 Nacos 配置导入 | 否 |
| `./peach.sh mysql:init` | 补导初始化数据 | 否 |
| `./peach.sh nacos:import` | 重新导入 Nacos 配置 | 否 |
| `./peach.sh nginx:reload` | 重新加载 `peach-front` 内的 Nginx | 否 |
| `./peach.sh clean:logs` | 删除日志文件 | 否 |
| `./peach.sh clean:data` | 删除 `runtime/data` 和 `runtime/upload` | 是 |

`clean:data` 会删除 MySQL、Redis、Nacos 和上传文件数据，只在确认数据可丢弃时使用。

旧命令习惯里如果传 `nginx`，脚本会自动映射到当前服务名 `peach-front`，例如 `./peach.sh logs nginx` 等价于 `./peach.sh logs peach-front`。

## 排障

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `./peach.sh build` 找不到 jar | Maven 打包是否成功 | 查看 Maven 输出；确认对应 `*-launch/target/*.jar` 存在 |
| `./peach.sh front:build` 失败 | `peach-cloud-front` 是否存在、Node 依赖是否可安装 | 进入 `peach-cloud-front` 执行 `npm ci` 看具体错误 |
| 前端页面 404 或白屏 | `peach-cloud/peach-front:${PEACH_IMAGE_TAG}` 是否包含 `index.html` | 执行 `docker run --rm peach-cloud/peach-front:${PEACH_IMAGE_TAG:-latest} ls /usr/share/nginx/html` |
| 前端能打开但接口不通 | `GATEWAY_PORT` 与 `peach-gateway` 的 `SERVER_PORT` 是否一致 | 检查 `.env` 和 `docker compose exec peach-front cat /etc/nginx/conf.d/default.conf` |
| WebSocket 断开 | 浏览器实际 WS 地址、Nginx `/api/` 代理和网关路由是否匹配 | 先看浏览器 Network，再看 `./peach.sh logs peach-gateway` |
| `Ports are not available` | 宿主机端口是否被占用 | 修改 `.env` 中对应 `*_HOST_PORT` 或 `NGINX_HTTP_PORT` |
| MySQL 认证失败 | `runtime/data/mysql` 是否已有旧密码 | 改回旧密码；只有数据可丢时再执行 `clean:data` |
| MySQL 表名大小写异常 | `lower_case_table_names` 是否为 `1` | 该参数必须在数据目录初始化前生效；数据可丢时重建 |
| 初始化菜单、用户、租户缺失 | `sql/INIT.sql` 是否导入 | 执行 `./peach.sh mysql:init` |
| Nacos 配置为空 | namespace、group 是否与 `.env` 一致 | 执行 `./peach.sh nacos:import`，再重启相关服务 |
| 业务服务退出 | Nacos、MySQL、Redis 地址和密码是否一致 | 执行 `./peach.sh logs <service>` |
| 修改 Nginx 模板后不生效 | 镜像是否重建 | 执行 `./peach.sh front:build` 后 `./peach.sh restart peach-front` |

## 与 Pipeline 方案的关系

`deploy-pipline/` 使用同样的运行模型：7 个后端镜像 + `peach-front` 镜像。差异如下：

| 项目 | 本地 Docker Desktop | GitLab/Jenkins Pipeline |
| --- | --- | --- |
| 后端镜像 | 本机 `./peach.sh build` | Jenkins Maven 构建并推送 Registry |
| 前端镜像 | 本机 `./peach.sh front:build` | Jenkins Node 构建并推送 Registry |
| 镜像名前缀 | `peach-cloud/...` | `registry:5000/peach-cloud/...` |
| 部署配置 | `deploy/.env` | Jenkins Secret file |
| 对外端口 | `deploy/.env` 的 `NGINX_HTTP_PORT` | 外层 `PEACH_APP_HTTP_PORT` 映射到 DinD 内部 80 |

## 验证命令

修改部署文件后建议执行：

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
node scripts/check-utf8.mjs
git diff --check
```
