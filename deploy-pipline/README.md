# GitLab + Jenkins + Registry 自动构建部署指南

本目录是独立的 CI/CD 部署方案：GitLab Webhook → Jenkins → Maven 打包 → 镜像推送 Registry → 启动 Peach Cloud。

**快速导航**

| 文档 | 内容 |
| --- | --- |
| 本文 | DevOps 栈启动、Jenkins/GitLab 配置、流水线说明 |
| [`docs/local-development.md`](docs/local-development.md) | **本地 Maven 依赖、刷新缓存、IDEA、Windows 命令** |
| [`docs/nginx-architecture.md`](docs/nginx-architecture.md) | 业务 Nginx 与 DevOps 子域分层 |
| [`maven/README.md`](maven/README.md) | Nexus 初始化、settings id 对应关系 |
| [`observability/README.md`](observability/README.md) | Prometheus、Tempo、Loki、Alloy、Grafana 部署和验证 |

原有 `deploy/` 目录仍保留本地 Docker Desktop 部署方式；本方案不要求修改 `deploy/.env` 或 `deploy/docker-compose.yml`。

## 文件职责

| 文件 | 用途 |
| --- | --- |
| `deploy-pipline/pipline/docker-compose.yml` | 启动 GitLab、Jenkins、Registry、Nexus、DevOps Nginx |
| `deploy-pipline/pipline/jenkins/Dockerfile` | Jenkins 镜像（Docker CLI、GitLab 插件、JDK 21） |
| `deploy-pipline/pipline/maven-node/Dockerfile` | Maven + Node CI 镜像 |
| `maven/settings.xml` | 唯一 Maven settings 模板；本地可复制，CI 自动渲染 |
| `deploy-pipline/scripts/render-maven-settings.mjs` | Jenkins 渲染 Maven settings 的脚本，避免在 Jenkinsfile 内联复杂 XML/正则 |
| `deploy-pipline/maven/README.md` | Nexus 与 settings 说明 |
| `deploy-pipline/docs/local-development.md` | 本地开发、依赖刷新、IDEA |
| `deploy-pipline/docs/nginx-architecture.md` | Nginx 分层方案 |
| `deploy-pipline/pipline/nginx/devops.conf` | DevOps 多域名反代（业务域仅前端） |
| `deploy-pipline/Jenkinsfile` | Jenkins 流水线定义 |
| `deploy-pipline/docker/Dockerfile.service` | 后端服务镜像 Dockerfile |
| `deploy-pipline/docker/Dockerfile.front` | 前端 Nginx 镜像 Dockerfile |
| `deploy-pipline/nginx/peach.conf.template` | 前端镜像内的 Nginx 模板，运行时读取 `GATEWAY_PORT` |
| `deploy-pipline/docker-compose.deploy.yml` | 流水线专用运行 Compose，只拉 Registry 镜像，不本地构建 |
| `deploy-pipline/import-nacos.sh` | 流水线专用 Nacos 配置导入脚本，支持 Jenkins Secret file；会导入 `.yml`、`.yaml` 和 `.json` |
| `deploy-pipline/peach-deploy.env.example` | Jenkins Secret file 的配置模板 |
| `deploy-pipline/observability/` | 可观测性组件配置和 Grafana 数据源预配置 |

## 选择部署的服务

Jenkins 构建参数提供受白名单约束的部署开关：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `DEPLOY_SERVICES` | 全部 8 个后端服务和前端 | 使用空格或逗号分隔，也可填写 `all`；未知服务会直接终止流水线 |
| `STOP_UNSELECTED_SERVICES` | `false` | 为 `true` 时停止未选择的业务服务，不影响 DevOps 和可观测性基础设施 |

例如，只发布网关、认证和前端：

```text
DEPLOY_SERVICES=peach-gateway peach-auth peach-front
STOP_UNSELECTED_SERVICES=true
```

流水线只构建和推送被选择服务的镜像，部署时使用 `--no-deps` 启动这些服务；MySQL、Redis、Nacos 仍作为业务运行基础设施启动。可观测性组件与 Jenkins 一起由 DevOps Compose 先启动，业务流水线不再复制或挂载观测配置。

流水线根据 `DEPLOY_SERVICES` 推送以下镜像中的选中项：

```text
localhost:5000/peach-cloud/peach-gateway:<git-sha>
localhost:5000/peach-cloud/peach-auth:<git-sha>
localhost:5000/peach-cloud/peach-monitor:<git-sha>
localhost:5000/peach-cloud/peach-fileservice:<git-sha>
localhost:5000/peach-cloud/peach-message:<git-sha>
localhost:5000/peach-cloud/peach-setting:<git-sha>
localhost:5000/peach-cloud/peach-generator:<git-sha>
localhost:5000/peach-cloud/peach-scheduled:<git-sha>
localhost:5000/peach-cloud/peach-front:<git-sha>
```

## 端口模型

这套方案只使用本机 Docker Desktop 这一套 Docker daemon。Jenkins 容器通过 `/var/run/docker.sock` 调用宿主机 Docker Desktop，因此流水线构建出的镜像和启动的容器会直接出现在本机 Docker Desktop。

| 层级 | 配置位置 | 含义 |
| --- | --- | --- |
| 宿主机到 DevOps Nginx | `deploy-pipline/pipline/docker-compose.yml` 的 `DEVOPS_HTTP_PORT` | 控制宿主机访问统一反向代理入口的端口，默认 80 |
| Peach Cloud 运行容器 | Jenkins Secret file，也就是 `peach-deploy.env` | 控制 `peach-gateway`、`peach-auth` 等容器内部监听端口 |

默认访问链路是：

```text
# 业务（用户）
http://peach_cloud.peachsoft.com
  -> peach-devops-nginx:80
  -> peach-front:80
  -> /api 代理到 peach-gateway:${GATEWAY_PORT}

# DevOps（运维书签，独立子域）
http://jenkins.peachsoft.com  -> Jenkins
http://nexus.peachsoft.com    -> Nexus UI（Maven 请直连 :8081）
http://grafana.peachsoft.com  -> Grafana
http://prometheus.peachsoft.com -> Prometheus

详见 deploy-pipline/docs/nginx-architecture.md
```

如果宿主机 80 端口冲突，启动 DevOps 服务前设置：

```bash
DEVOPS_HTTP_PORT=18088 docker compose -f deploy-pipline/pipline/docker-compose.yml up -d
```

Jenkins Secret file 里不需要再配置 `NGINX_HTTP_PORT`。`peach-front` 不发布宿主机 80 端口，外层由 `peach-devops-nginx` 代理到 Docker Desktop 中的 `peach-front:80`。当前没有配置 HTTPS server block；需要 HTTPS 时要补证书挂载和 Nginx 443 配置。

## 首次启动 DevOps 服务

先复制环境变量模板并替换所有 `change_me_*` 值，再在仓库根目录执行：

```bash
cp deploy-pipline/peach-deploy.env.example deploy-pipline/peach-deploy.env
docker compose --env-file deploy-pipline/peach-deploy.env -f deploy-pipline/pipline/docker-compose.yml config
docker compose --env-file deploy-pipline/peach-deploy.env -f deploy-pipline/pipline/docker-compose.yml build jenkins
docker compose --env-file deploy-pipline/peach-deploy.env -f deploy-pipline/pipline/docker-compose.yml up -d
docker compose --env-file deploy-pipline/peach-deploy.env -f deploy-pipline/pipline/docker-compose.yml ps
```

必须先完成这一步再运行 Jenkins 业务流水线。流水线会检查 `peach-devops` 网络及六个观测容器，任一组件未运行都会在业务部署前终止。

启动后确认 Jenkins 能访问本机 Docker Desktop：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins docker version
```

默认入口：

| 服务 | 地址 |
| --- | --- |
| Peach Cloud 前端 | `http://peach_cloud.peachsoft.com` |
| Jenkins | `http://jenkins.peachsoft.com` |
| GitLab | `http://gitlab.peachsoft.com` |
| Registry UI | `http://registry.peachsoft.com` |
| Registry API | `http://registry.peachsoft.com/v2/` |
| Nexus | `http://nexus.peachsoft.com` 或 `http://localhost:8081` |
| Grafana | `http://grafana.peachsoft.com` 或 `http://localhost:3000` |
| Prometheus | `http://prometheus.peachsoft.com` 或 `http://127.0.0.1:9090` |
| Nacos（DevOps 子域） | `http://nacos.peachsoft.com/nacos/` |
| Nacos（部署后，经前端容器） | `http://peach_cloud.peachsoft.com/nacos/`（需 Deploy 完成且 `peach-front` 运行） |

直连兜底地址仍保留：

| 服务 | 地址 |
| --- | --- |
| Jenkins | `http://localhost:8080` |
| GitLab | `http://localhost:8929` |
| Registry API | `http://localhost:5000/v2/` |
| Registry UI | `http://localhost:5001` |

Windows hosts 需要增加：

```text
127.0.0.1 peach_cloud.peachsoft.com
127.0.0.1 jenkins.peachsoft.com
127.0.0.1 gitlab.peachsoft.com
127.0.0.1 registry.peachsoft.com
127.0.0.1 nacos.peachsoft.com
127.0.0.1 nexus.peachsoft.com
127.0.0.1 grafana.peachsoft.com
127.0.0.1 prometheus.peachsoft.com
```

如果设置了 `DEVOPS_HTTP_PORT=18088`，浏览器地址需要带端口，例如 `http://peach_cloud.peachsoft.com:18088`。

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

把这份真实 `.env` 直接放到 Jenkins Secret file 是当前推荐方式。注意边界：

- 可以放：数据库、Redis、Nacos、对象存储、Nexus、Maven 代理等流水线需要的环境变量。
- 不要放到 Git：真实密码、token、生产地址、代理凭据。
- 不上传真实 Maven `settings.xml`；仓库内 `maven/settings.xml` 只作为模板。Jenkinsfile 会在 Maven 容器启动前填充变量，生成 `/var/jenkins_home/.m2/settings.generated.xml`，再通过 Jenkins 持久卷挂载给 Maven 容器读取。
- `.env` 会被 Jenkins shell source；包含 `|`、空格、`#`、`&` 等特殊字符的值必须加引号，例如 `MAVEN_PROXY_NON_PROXY_HOSTS="localhost|127.0.0.1|nexus"`。

至少确认这些变量：

```dotenv
PEACH_LOG_ROOT=/host_mnt/c/path/to/peach-cloud/runtime/logs
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
SCHEDULED_PORT=18087
MAVEN_LOCAL_REPOSITORY=/var/jenkins_home/.m2/repository
MAVEN_NEXUS_URL=http://nexus:8081
MAVEN_ALIYUN_PUBLIC_URL=https://maven.aliyun.com/repository/public
PEACH_NEXUS_URL=http://nexus:8081
MAVEN_NEXUS_USERNAME=Development
MAVEN_NEXUS_PASSWORD=change_me_nexus_password
```

不要把真实密码、token 或生产地址写入仓库文件。

## 前端镜像与 Nginx

前端现在也是镜像。`Build frontend` 阶段会执行：

```bash
npm ci
npm run build
docker build -f deploy-pipline/docker/Dockerfile.front -t localhost:5000/peach-cloud/peach-front:<git-sha> deploy-pipline
docker push localhost:5000/peach-cloud/peach-front:<git-sha>
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
| `/nacos/` | 部署后由 `peach-front` 容器代理到 `nacos:8848`（非 DevOps Nginx） |

当前前端代码未设置 `VITE_API_BASE_URL` 时使用 `/api`，未设置 `VITE_WS_BASE_URL` 时使用当前页面 host 拼 `/api/webSocket/message`。因此浏览器访问外层端口即可同时访问前端、接口和 WebSocket。

## CI 构建环境（JDK / Maven）

流水线 Maven 打包阶段使用仓库内 CI 镜像，与根 `pom.xml` 的 **Java 21** 保持一致：

| 组件 | 版本 |
| --- | --- |
| JDK | Eclipse Temurin **21** |
| Maven | **3.9.11** |
| Node.js | **22**（CI 镜像内，供前端构建阶段使用） |

镜像定义：`deploy-pipline/pipline/maven-node/Dockerfile`
镜像标签：`peach-ci/maven-node:3.9.11-eclipse-temurin-21-node22`（由 `Jenkinsfile` 的 `Build CI image` 阶段构建）

### Java 21 CI 构建环境确认

当前 CI 基线必须与根 `pom.xml` 的 `java.version=21` 保持一致。首次部署、Jenkins 缓存异常或 CI 镜像变更后，按顺序确认：

1. 拉取包含当前 `Jenkinsfile` 和 `maven-node/Dockerfile` 的代码并推送到 GitLab 部署分支。
2. 在仓库根目录手动重建当前 CI 镜像（可选，流水线 `Build CI image` 阶段也会重建）：

```bash
docker build \
  -f deploy-pipline/pipline/maven-node/Dockerfile \
  -t peach-ci/maven-node:3.9.11-eclipse-temurin-21-node22 \
  deploy-pipline/pipline/maven-node
```

3. 在 Jenkins 中对 Pipeline 任务执行 **Build Now**。
4. 确认 `Build CI image` 与 `Maven package` 阶段成功，Maven 日志应显示 Java 21 编译；失败时优先检查 Java 版本、依赖解析和仓库凭证。
5. 若 Maven 依赖解析异常，可在 Jenkins 容器内清理本地仓库后重试（会重新下载依赖）：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins rm -rf /var/jenkins_home/.m2/repository
```

> 不要回退到旧 JDK 基线 CI 镜像；当前项目源码、依赖和 Maven Enforcer 均以 Java 21 为基线。

## Maven 私服（Nexus）与 settings

策略：依赖解析优先访问 Nexus `maven-public`，Nexus 连接不上时第三方依赖回退到阿里云 `public` 仓库。私有 `com.peach` 构件和发布仍依赖 Nexus。仓库只维护 `maven/settings.xml` 一份模板，本地复制使用，Jenkins 自动渲染。

| 项 | 值 |
| --- | --- |
| 本地 Maven URL | `http://nexus.peachsoft.com:8081/repository/maven-public/` |
| CI 内部 URL | `http://nexus:8081/repository/maven-public/` |
| settings `<id>` | 优先下载 **`nexus-public`**；回退 `aliyun-public`；发布 `peach-releases` / `peach-snapshots` |
| 本地 settings | 复制 `maven/settings.xml` → `~/.m2/settings.xml` |
| CI settings | `Jenkinsfile` 根据 `maven/settings.xml` 生成 `/var/jenkins_home/.m2/settings.generated.xml` |
| CI Nexus 账号 | 在 Jenkins Secret file `.env` 中配置 `MAVEN_NEXUS_USERNAME` / `MAVEN_NEXUS_PASSWORD`；为空时不生成 `<servers>` |
| CI Maven 代理 | 在 Jenkins Secret file `.env` 中配置 `MAVEN_PROXY_HOST` / `MAVEN_PROXY_PORT`；为空时不生成 `<proxies>` |

**本地依赖刷新**（改版本、清缓存、IDEA 重导）：见 [`docs/local-development.md`](docs/local-development.md)。

### Nexus 首次部署

1. `docker compose -f deploy-pipline/pipline/docker-compose.yml up -d nexus`
2. 初始密码：`docker exec nexus cat /nexus-data/admin.password`
3. 创建 `aliyun-public` proxy + `maven-public` group（成员含 `aliyun-public`、`maven-releases`、`maven-snapshots`）
4. 配置用户或开启 Anonymous Read
5. 本地复制 `settings.xml`，替换 `@MAVEN_*@` 占位符；不需要账号或代理时删除对应 `@optional` 注释块，再执行 `mvn dependency:get -Dartifact=org.springframework.boot:spring-boot-starter:3.5.4`

### Jenkins 清 CI 依赖缓存

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins rm -rf /var/jenkins_home/.m2/repository
```

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
| Build CI image | 通过 Docker Desktop 构建 `peach-ci/maven-node:3.9.11-eclipse-temurin-21-node22`（Maven 3.9.11 + JDK 21 + Node 22） |
| Maven package | 从 `settings.xml` 渲染 `settings.generated.xml`，按 Nexus → 阿里云顺序解析依赖，执行 `mvn -B -Pdocker -Dpeach.nexus.url=${PEACH_NEXUS_URL:-http://nexus:8081} -DskipTests clean package` |
| Build and push images | 只构建并推送 `DEPLOY_SERVICES` 选中的后端镜像 |
| Build frontend | 仅选择 `peach-front` 时构建前端 dist 并推送镜像 |
| Deploy | 同步 Compose、观测组件配置、Nacos 脚本和 SQL，启动选中服务及可选观测栈 |

Deploy 阶段会把 `deploy/nacos/config` 同步到 Jenkins 持久目录，并通过 `deploy-pipline/import-nacos.sh` 导入 `.yml`、`.yaml` 和 `.json`。以下两个 Sentinel 规则文件也会作为 Nacos 配置导入：

```text
peach-openfeign-sentinel-flow-rules.json
peach-openfeign-sentinel-degrade-rules.json
```

它们的 Data ID 必须和 `peach-openfeign.yml` 中的 `spring.cloud.sentinel.datasource.*.nacos.data-id` 保持一致。Sentinel Nacos 数据源使用独立客户端，`peach-openfeign.yml` 会通过流水线注入的 `SPRING_CLOUD_NACOS_CONFIG_*` / `NACOS_*` 显式传递服务地址和 namespace。

部署目录固定为：

```text
/var/jenkins_home/peach-cloud-deploy
```

该目录位于 Jenkins 持久卷，只保存部署脚本、Nacos 配置和 SQL。MySQL、Redis、Nacos 数据及上传文件继续使用 Docker named volumes；基础设施和业务日志通过 `PEACH_LOG_ROOT` 绑定到 Docker 宿主目录，不随 Jenkins workspace 清理。

Docker Desktop 场景中，Jenkins 通过 Docker socket 调用宿主引擎，不能把 `/var/jenkins_home/...` 当作 bind mount 源目录。`PEACH_LOG_ROOT` 必须填写 Docker 引擎可见的绝对路径。例如仓库位于 `C:\work\peach-cloud` 时填写 `/host_mnt/c/work/peach-cloud/runtime/logs`。DevOps Compose 和 Jenkins Secret file 必须使用同一个值，Alloy 才能直接读取业务日志。

## 验证

检查 DevOps 容器：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml ps
```

检查 Jenkins 访问的 Docker Desktop daemon：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins docker ps
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins docker images 'localhost:5000/peach-cloud/*'
curl http://peach_cloud.peachsoft.com/
curl http://localhost:5000/v2/_catalog
```

Registry UI 中应看到本次选择服务对应的镜像仓库；历史构建产生的其他仓库不会自动删除。

## 回滚

常规回滚方式是在 Jenkins 中找到历史成功构建，重新构建对应提交。流水线会重新生成或复用该提交对应的镜像标签，并更新容器。

不要通过删除 Docker named volumes 处理发布失败。`peach-mysql-data`、`peach-redis-data`、`peach-nacos-data` 和 `peach-fileservice-upload` 包含数据库、中间件和上传文件数据，只有确认数据可丢弃时才允许清理。

## 常见问题

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| Jenkins 构建找不到 `deploy-pipline/Jenkinsfile` | Pipeline Script Path | 确认填的是 `deploy-pipline/Jenkinsfile` |
| `fatal: not in a git directory` 且堆栈包含 `GitSCMFileSystem` | Jenkins 的 Lightweight checkout SCM 缓存已损坏 | 在任务配置里取消勾选 `Lightweight checkout`，保存后重新构建；流水线已在 `checkout scm` 前自动清理当前 workspace，但这类报错发生在 Jenkins 读取 `Jenkinsfile` 之前，不能靠流水线自身先删 `caches`。仍失败时先重建 Jenkins 镜像，再在 Jenkins 容器内执行 `clean-jenkins-scm-cache.sh <job-name>` 后重试 |
| Jenkins 镜像构建失败，找不到 Docker CLI | Jenkins 镜像是否由 `deploy-pipline/pipline/jenkins/Dockerfile` 构建 | 重新执行 `docker compose -f deploy-pipline/pipline/docker-compose.yml build jenkins` |
| Jenkins 执行 `docker ps` 失败 | Docker Desktop socket 是否挂载进 Jenkins | 确认 Docker Desktop 已启动，且 `jenkins` 服务挂载了 `/var/run/docker.sock:/var/run/docker.sock` |
| Maven 依赖下载很慢 | Nexus / `maven-public` group / settings | 见 [`maven/README.md`](maven/README.md) 与 [`docs/local-development.md`](docs/local-development.md) |
| Maven 401 | Nexus 账号与 settings `<server>` id | 确认 `nexus-public` 与仓库 id 一致；CI 在 Secret file 中设置 `MAVEN_NEXUS_USERNAME` / `MAVEN_NEXUS_PASSWORD`；匿名读开启时可不配置 |
| Maven 仍用旧依赖 | 本地或 CI 缓存 | 本地 `mvn -U` 或删 `D:/Environment/repository/...`；CI 清 `/var/jenkins_home/.m2/repository` |
| Maven 阶段报 settings 模板不存在 | `MAVEN_SETTINGS_TEMPLATE` 路径 | 默认使用 `$WORKSPACE/deploy-pipline/maven/settings.xml`；自定义路径需在 Jenkins 可访问 |
| Nexus 停止后私有构件拉不到 | `com.peach` 构件是否只存在 Nexus | 启动 Nexus 或先发布对应构件；阿里云只作为第三方依赖回退 |
| Maven 日志中文乱码 | Maven JVM、forked javac 和 CI 镜像编码/语言是否一致 | 仓库已通过 `.mvn/jvm.config` 固定 UTF-8 + English diagnostics；CI 需执行 `Build CI image` 阶段重建，确保 `LANG`、`LC_ALL`、`MAVEN_OPTS`、`JAVA_TOOL_OPTIONS` 生效 |
| Maven 阶段报 `Cannot run program "node"` | 是否使用 `peach-ci/maven-node:3.9.11-eclipse-temurin-21-node22` | 执行 `Build CI image` 阶段重建 CI 镜像 |
| Maven 成功但后端镜像构建找不到 jar | `*-launch/target/*.jar` 是否存在 | 检查 Maven 阶段输出和模块打包结果 |
| 前端页面 404 或白屏 | `peach-front` 镜像是否包含 `index.html` | 检查 `Build frontend` 阶段和 `docker run --rm <image> ls /usr/share/nginx/html` |
| 前端能打开但接口不通 | `GATEWAY_PORT` 与 `peach-gateway` 的 `SERVER_PORT` 是否一致 | 检查 Secret file、`docker-compose.deploy.yml` 和 `peach-front` 容器渲染后的 Nginx 配置 |
| WebSocket 连接失败 | 前端实际 WS 地址、Nginx `/api/` 代理和网关 WebSocket 路由是否匹配 | 先看浏览器 Network 中的 WS 地址，再核对网关路由 |
| 推送 Registry 报 HTTP/HTTPS 协议错误 | 镜像标签是否仍使用 `registry:5000` | Docker Desktop 模式下使用 `localhost:5000/peach-cloud/...`，不要使用 `registry:5000` 作为构建产物标签 |
| Webhook 无法访问 Jenkins | GitLab outbound policy 和 Docker 网络 | 确认服务都在 `peach-devops` 网络，只放行 Jenkins 地址 |
| 主机访问不到 Peach Cloud | `DEVOPS_HTTP_PORT`、hosts 解析、运行网络和 `peach-front` 容器状态 | 检查 hosts 是否指向 `127.0.0.1`，检查 `peach-devops-nginx` 是否已连接到 `peach-cloud-runtime`，检查 `peach-front` 是否运行 |
| Nacos 代理 502 | Peach Cloud 运行 Compose 是否已启动 Nacos | Jenkins 完成 Deploy 后，`peach-front` 才能代理 `/nacos/`；DevOps 入口用 `nacos.peachsoft.com` |
| 业务日志持续出现 `Client not connected, current status:STARTING`，并连接 `127.0.0.1:9848` | 部署到 Nacos 的 `peach-openfeign.yml` 是否包含 Sentinel 数据源地址 | 重新执行 Deploy 导入最新配置并重启受影响服务；同时确认 Secret file 中 `NACOS_SERVER_ADDR` 指向 Compose 服务名而不是 localhost |
| Compose 提示 `PEACH_LOG_ROOT` 未设置或日志目录为空 | DevOps 启动环境与 Jenkins Secret file 是否使用同一宿主绝对路径 | Docker Desktop 使用 `/host_mnt/<drive>/.../runtime/logs`，不要使用 Jenkins 容器内的 `/var/jenkins_home/...` |
| `import-nacos.sh: Permission denied` | Jenkins 工作区或 GitLab 仓库未保留 shell 脚本执行位 | 当前 Jenkinsfile 会在复制后执行 `chmod +x`，并通过 `sh import-nacos.sh` 调用；更新流水线后重新构建 |
| MySQL 密码修改后仍认证失败 | 持久数据目录已有旧密码 | 使用旧密码；只有确认数据可丢时再清理数据 |

## 已知限制

| 项 | 说明 |
| --- | --- |
| HTTPS | DevOps Nginx 当前仅 HTTP；生产需补 443 与证书 |
| Registry | 本地 HTTP 无认证，仅适合可信内网 |

## 生产边界

当前 Registry 是 HTTP 且无认证，只适合隔离可信内网。生产环境需要补齐 TLS、认证、镜像清理策略和访问控制。

Jenkins 挂载 Docker Desktop socket 后可以控制本机 Docker daemon，等同高权限基础设施。本方案中 Jenkins 容器以 root 用户运行，用于避免 Docker socket 权限不一致导致流水线无法执行 `docker` 命令；应限制 Jenkins 管理员、任务配置和部署分支写权限。

当前发布方式是单机重建式更新，会有短暂停机。需要零停机、灰度、自动迁移和多节点调度时，应迁移到 Kubernetes、Docker Swarm 或专门部署节点。

## 本地验证命令

修改本目录后建议执行：

```bash
docker compose -f deploy-pipline/pipline/docker-compose.yml config
docker compose --env-file deploy-pipline/peach-deploy.env.example -f deploy-pipline/docker-compose.deploy.yml config
node scripts/check-utf8.mjs
git diff --check
```


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
