# GitLab + Jenkins + Registry 自动构建部署指南

本目录是独立的 CI/CD 部署方案，用于通过 GitLab Webhook 触发 Jenkins，完成代码拉取、Maven 打包、后端镜像构建、前端镜像构建、推送 Registry，并启动 Peach Cloud。

原有 `deploy/` 目录仍然保留本地 Docker Desktop 部署方式；本方案不要求修改 `deploy/.env`、`deploy/peach.sh` 或 `deploy/docker-compose.yml`。

## 文件职责

| 文件 | 用途 |
| --- | --- |
| `deploy-pipline/pipline/docker-compose.yml` | 启动 GitLab、Jenkins、Registry、Registry UI、DevOps Nginx 和 Jenkins 专用 Docker Engine |
| `deploy-pipline/pipline/jenkins/Dockerfile` | 构建带 Docker CLI、Compose 插件、Node、Maven 和 GitLab 插件的 Jenkins 镜像 |
| `deploy-pipline/pipline/maven-node/Dockerfile` | 构建流水线 Maven 阶段使用的 Maven + Node CI 镜像 |
| `deploy-pipline/pipline/nginx/devops.conf` | DevOps 入口 Nginx 配置，按域名代理 Jenkins、GitLab、Registry、Nacos 和 Peach Cloud |
| `deploy-pipline/Jenkinsfile` | Jenkins 流水线定义 |
| `deploy-pipline/docker/Dockerfile.service` | 后端服务镜像 Dockerfile |
| `deploy-pipline/docker/Dockerfile.front` | 前端 Nginx 镜像 Dockerfile |
| `deploy-pipline/nginx/peach.conf.template` | 前端镜像内的 Nginx 模板，运行时读取 `GATEWAY_PORT` |
| `deploy-pipline/docker-compose.deploy.yml` | 流水线专用运行 Compose，只拉 Registry 镜像，不本地构建 |
| `deploy-pipline/import-nacos.sh` | 流水线专用 Nacos 配置导入脚本，支持 Jenkins Secret file；会导入 `.yml`、`.yaml` 和 `.json` |
| `deploy-pipline/peach-deploy.env.example` | Jenkins Secret file 的配置模板 |

流水线会推送这些镜像：

```text
registry:5000/peach-cloud/peach-gateway:<git-sha>
registry:5000/peach-cloud/peach-auth:<git-sha>
registry:5000/peach-cloud/peach-monitor:<git-sha>
registry:5000/peach-cloud/peach-fileservice:<git-sha>
registry:5000/peach-cloud/peach-message:<git-sha>
registry:5000/peach-cloud/peach-setting:<git-sha>
registry:5000/peach-cloud/peach-generator:<git-sha>
registry:5000/peach-cloud/peach-front:<git-sha>
```

## 端口模型

这套方案有两层 Docker：

| 层级 | 配置位置 | 含义 |
| --- | --- | --- |
| 宿主机到 DevOps Nginx | `deploy-pipline/pipline/docker-compose.yml` 的 `DEVOPS_HTTP_PORT` | 控制宿主机访问统一反向代理入口的端口，默认 80 |
| DinD 内部服务端口 | Jenkins Secret file，也就是 `peach-deploy.env` | 控制 `peach-gateway`、`peach-auth` 等容器内部监听端口 |

默认访问链路是：

```text
http://peachsoft.peach-cloud.test
  -> peach-devops-nginx:80
  -> jenkins-docker:80
  -> peach-front:80
  -> /api 代理到 peach-gateway:${GATEWAY_PORT}
```

如果宿主机 80 端口冲突，启动 DevOps 服务前设置：

```bash
DEVOPS_HTTP_PORT=18088 docker compose -f deploy-pipline/pipline/docker-compose.yml up -d
```

Jenkins Secret file 里不需要再配置 `NGINX_HTTP_PORT`。`peach-front` 在 Jenkins DinD 内固定监听 80，外层由 `peach-devops-nginx` 代理到 `jenkins-docker:80`。当前没有配置 HTTPS server block；需要 HTTPS 时要补证书挂载和 Nginx 443 配置。

## 首次启动 DevOps 服务

在仓库根目录执行：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml config
docker compose -f deploy-pipline/pipline/docker-compose.yml build jenkins
docker compose -f deploy-pipline/pipline/docker-compose.yml up -d
docker compose -f deploy-pipline/pipline/docker-compose.yml ps
```

默认入口：

| 服务 | 地址 |
| --- | --- |
| Peach Cloud 前端 | `http://peachsoft.peach-cloud.test` |
| Jenkins | `http://peachsoft.jenkins.test` |
| GitLab | `http://peachsoft.gitlab.test` |
| Registry UI | `http://peachsoft.registry.test` |
| Registry API | `http://peachsoft.registry.test/v2/` |
| Nacos | `http://peachsoft.nacos.test/nacos/` 或 `http://peachsoft.peach-cloud.test/nacos/` |

直连兜底地址仍保留：

| 服务 | 地址 |
| --- | --- |
| Jenkins | `http://localhost:8080` |
| GitLab | `http://localhost:8929` |
| Registry API | `http://localhost:5000/v2/` |
| Registry UI | `http://localhost:5001` |

Windows hosts 需要增加：

```text
127.0.0.1 peachsoft.peach-cloud.test
127.0.0.1 peachsoft.jenkins.test
127.0.0.1 peachsoft.gitlab.test
127.0.0.1 peachsoft.registry.test
127.0.0.1 peachsoft.nacos.test
```

如果设置了 `DEVOPS_HTTP_PORT=18088`，浏览器地址需要带端口，例如 `http://peachsoft.peach-cloud.test:18088`。

首次获取 Jenkins 管理员密码：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

## Jenkins 凭据

进入 `Manage Jenkins -> Credentials -> System -> Global credentials`。

GitLab 拉取凭据建议创建 `Username with password`：

```text
ID: gitlab-peach-cloud
Username: 有项目读取权限的 GitLab 用户
Password: GitLab Personal Access Token，最小权限 read_repository
```

部署配置创建 `Secret file`。它不是 key/value 表单，而是上传一个 `.env` 文件：

```text
ID: peach-deploy-env
File: 基于 deploy-pipline/peach-deploy.env.example 复制并修改后的 env 文件
```

创建步骤：

1. 在本机基于 `deploy-pipline/peach-deploy.env.example` 准备一份真实 `.env` 文件，替换其中的 `change_me_*` 占位值。
2. 进入 `Manage Jenkins -> Credentials -> System -> Global credentials -> Add Credentials`。
3. `Kind` 选择 `Secret file`。
4. `File` 上传这份 `.env` 文件。
5. `ID` 必须填写 `peach-deploy-env`，不能留空，也不能写成别的名字。
6. 保存后重新执行流水线。

至少确认这些变量：

```dotenv
MYSQL_ROOT_PASSWORD=change_me_mysql_root_password
MYSQL_DATABASE=peach_cloud
REDIS_PASSWORD=change_me_redis_password
OSS_ACCESS_KEY=change_me_oss_access_key
OSS_SECRET_KEY=change_me_oss_secret_key
COS_ACCESS_KEY=change_me_cos_access_key
COS_SECRET_KEY=change_me_cos_secret_key
NACOS_NAMESPACE_ID=peach-cloud
NACOS_NAMESPACE_NAME=peach-cloud
NACOS_GROUP=PEACH-CLOUD
GATEWAY_PORT=18080
AUTH_PORT=18081
MONITOR_PORT=18082
FILESERVICE_PORT=18083
MESSAGE_PORT=18084
SETTING_PORT=18085
GENERATOR_PORT=18086
```

不要把真实密码、token 或生产地址写入仓库文件。

## 前端镜像与 Nginx

前端现在也是镜像。`Build frontend` 阶段会执行：

```bash
npm ci
npm run build
docker build -f deploy-pipline/docker/Dockerfile.front -t registry:5000/peach-cloud/peach-front:<git-sha> deploy-pipline
docker push registry:5000/peach-cloud/peach-front:<git-sha>
```

`Dockerfile.front` 基于 `nginx:1.25-alpine`，把两类内容打进镜像：

| 内容 | 镜像内位置 |
| --- | --- |
| `peach-cloud-front/dist` | `/usr/share/nginx/html/` |
| `deploy-pipline/nginx/peach.conf.template` | `/etc/nginx/templates/default.conf.template` |

Nginx 官方镜像启动时会把模板渲染成实际配置。`GATEWAY_PORT` 来自 `docker-compose.deploy.yml` 的环境变量，因此如果 Secret file 里把网关端口从 `18080` 改成别的值，前端 Nginx 代理也会跟着变。

当前代理关系：

| 访问路径 | Nginx 行为 |
| --- | --- |
| `/` | 读取前端 dist，`try_files` 回退到 `index.html`，支持 Vue Router |
| `/api/` | 代理到 `peach-gateway:${GATEWAY_PORT}/api/` |
| `/api/doc.html`、`/doc.html`、`/webjars/`、`/v3/api-docs/` | 代理接口文档资源到网关 |
| `/webSocket/` | 代理到 `peach-gateway:${GATEWAY_PORT}/webSocket/` |
| `/api/webSocket/...` | 命中 `/api/` 代理，再由网关处理 |
| `/nacos/` | 代理到 `nacos:8848/nacos/` |

当前前端代码未设置 `VITE_API_BASE_URL` 时使用 `/api`，未设置 `VITE_WS_BASE_URL` 时使用当前页面 host 拼 `/api/webSocket/message`。因此浏览器访问外层端口即可同时访问前端、接口和 WebSocket。

## Jenkins Pipeline

创建 Pipeline 任务：

1. `New Item`，类型选择 `Pipeline`。
2. `Build Triggers` 勾选 `Build when a change is pushed to GitLab`。
3. 保存 Jenkins 页面显示的 GitLab Webhook URL 和 Secret Token。
4. `Pipeline Definition` 选择 `Pipeline script from SCM`。
5. SCM 选择 Git，Repository URL 建议使用容器内地址：

```text
http://gitlab/<group>/<project>.git
```

例如当前项目：

```text
http://gitlab/peachsoft/peach-cloud.git
```

不要继续使用 `http://gitlab:8929/...`。当前 GitLab 容器内监听 80，宿主机兜底端口 `8929` 是 Docker 端口映射，不是 Jenkins 容器访问 GitLab 的内部端口。

6. Credentials 选择 `gitlab-peach-cloud`。
7. Branch Specifier 填部署分支，例如 `*/main`。
8. Script Path 填：

```text
deploy-pipline/Jenkinsfile
```

先手动执行一次 `Build Now`，确认流水线完整跑通后再接 Webhook。

## GitLab Webhook

进入 GitLab 项目 `Settings -> Webhooks`：

```text
URL: Jenkins 任务页面给出的 URL，通常是 http://jenkins:8080/project/peach-cloud-deploy
Secret token: Jenkins 任务生成的 token
Trigger: Push events
Branch filter: 只允许部署分支，例如 main
SSL verification: 当前 HTTP 内网地址不需要；改 HTTPS 后必须开启验证
```

如果 GitLab 测试 Webhook 提示 `Requests to the local network are not allowed`，进入 GitLab 管理后台 `Settings -> Network -> Outbound requests`，只放行受信任 Jenkins 内网地址。

## 流水线机制

| 阶段 | 行为 |
| --- | --- |
| Checkout | 从 GitLab 拉取触发提交，计算 12 位短 SHA |
| Build CI image | 在 Jenkins DinD 中构建 `peach-ci/maven-node:3.9.11-eclipse-temurin-8-node22` |
| Maven package | 使用 Maven + Node CI 镜像执行 `mvn -B -Pdocker -DskipTests clean package` |
| Build and push images | 构建并推送 7 个后端镜像 |
| Build frontend | 构建前端 dist，打包并推送 `peach-front` 镜像 |
| Deploy | 同步专用 Compose、Nacos 脚本和 SQL 到 Jenkins 持久目录，拉取镜像并启动容器 |

Deploy 阶段会把 `deploy/nacos/config` 同步到 Jenkins 持久目录，并通过 `deploy-pipline/import-nacos.sh` 导入 `.yml`、`.yaml` 和 `.json`。以下两个 Sentinel 规则文件也会作为 Nacos 配置导入：

```text
peach-openfeign-sentinel-flow-rules.json
peach-openfeign-sentinel-degrade-rules.json
```

它们的 Data ID 必须和 `peach-openfeign.yml` 中的 `spring.cloud.sentinel.datasource.*.nacos.data-id` 保持一致。

部署目录固定为：

```text
/var/jenkins_home/peach-cloud-deploy
```

该目录位于 Jenkins 持久卷，Jenkins 清理 workspace 时不会删除 MySQL、Redis、Nacos 和上传文件。

## 验证

检查 DevOps 容器：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml ps
```

检查 Jenkins 内部 Docker Engine：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins docker ps
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins docker images 'registry:5000/peach-cloud/*'
curl http://peachsoft.peach-cloud.test/
curl http://localhost:5000/v2/_catalog
```

Registry UI 中应看到 8 个镜像仓库：7 个后端服务和 `peach-front`。

## 回滚

常规回滚方式是在 Jenkins 中找到历史成功构建，重新构建对应提交。流水线会重新生成或复用该提交对应的镜像标签，并更新容器。

不要通过删除运行期数据目录处理发布失败。`/var/jenkins_home/peach-cloud-deploy/deploy/runtime/data` 包含数据库和中间件数据，只有确认数据可丢弃时才允许清理。

## 常见问题

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| Jenkins 构建找不到 `deploy-pipline/Jenkinsfile` | Pipeline Script Path | 确认填的是 `deploy-pipline/Jenkinsfile` |
| Jenkins 镜像构建失败，找不到 Docker CLI | Jenkins 镜像是否由 `deploy-pipline/pipline/jenkins/Dockerfile` 构建 | 重新执行 `docker compose -f deploy-pipline/pipline/docker-compose.yml build jenkins` |
| Maven 阶段报 `Cannot run program "node"` | Maven 阶段是否仍在使用纯 `maven:3.9.11-eclipse-temurin-8` 镜像 | 使用本仓库 `Jenkinsfile` 的 `Build CI image` 阶段，确保 Maven package 使用 `peach-ci/maven-node:3.9.11-eclipse-temurin-8-node22` |
| Maven 成功但后端镜像构建找不到 jar | `*-launch/target/*.jar` 是否存在 | 检查 Maven 阶段输出和模块打包结果 |
| 前端页面 404 或白屏 | `peach-front` 镜像是否包含 `index.html` | 检查 `Build frontend` 阶段和 `docker run --rm <image> ls /usr/share/nginx/html` |
| 前端能打开但接口不通 | `GATEWAY_PORT` 与 `peach-gateway` 的 `SERVER_PORT` 是否一致 | 检查 Secret file、`docker-compose.deploy.yml` 和 `peach-front` 容器渲染后的 Nginx 配置 |
| WebSocket 连接失败 | 前端实际 WS 地址、Nginx `/api/` 代理和网关 WebSocket 路由是否匹配 | 先看浏览器 Network 中的 WS 地址，再核对网关路由 |
| 推送 Registry 报 HTTP/HTTPS 协议错误 | DinD 是否配置 insecure registry | 检查 `jenkins-docker` 参数 `--insecure-registry=registry:5000` |
| Webhook 无法访问 Jenkins | GitLab outbound policy 和 Docker 网络 | 确认服务都在 `peach-devops` 网络，只放行 Jenkins 地址 |
| 主机访问不到 Peach Cloud | `DEVOPS_HTTP_PORT`、hosts 解析和 `peach-front` 容器状态 | 检查 hosts 是否指向 `127.0.0.1`，检查 `peach-devops-nginx` 和 Jenkins DinD 内的 `peach-front` 是否运行 |
| Nacos 代理 502 | Peach Cloud 运行 Compose 是否已启动 Nacos | Jenkins 首次完成 Deploy 后，`jenkins-docker:8849` 才会有 Nacos 服务 |
| `import-nacos.sh: Permission denied` | Jenkins 工作区或 GitLab 仓库未保留 shell 脚本执行位 | 当前 Jenkinsfile 会在复制后执行 `chmod +x`，并通过 `sh import-nacos.sh` 调用；更新流水线后重新构建 |
| MySQL 密码修改后仍认证失败 | 持久数据目录已有旧密码 | 使用旧密码；只有确认数据可丢时再清理数据 |

## 生产边界

当前 Registry 是 HTTP 且无认证，只适合隔离可信内网。生产环境需要补齐 TLS、认证、镜像清理策略和访问控制。

Jenkins Docker-in-Docker 使用特权模式，等同高权限基础设施。应限制 Jenkins 管理员、任务配置和部署分支写权限。

当前发布方式是单机重建式更新，会有短暂停机。需要零停机、灰度、自动迁移和多节点调度时，应迁移到 Kubernetes、Docker Swarm 或专门部署节点。

## 本地验证命令

修改本目录后建议执行：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml config
docker compose --env-file deploy-pipline/peach-deploy.env.example -f deploy-pipline/docker-compose.deploy.yml config
node scripts/check-utf8.mjs
git diff --check
```
