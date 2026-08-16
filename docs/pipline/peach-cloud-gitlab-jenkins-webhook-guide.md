# Peach Cloud GitLab + Jenkins + Webhook 自动构建部署指南

> 适用项目：`peach-cloud`  
> 适用环境：Windows + Docker Desktop + Docker Compose + GitLab CE + Jenkins  
> 目标：代码推送到 GitLab 后，通过 Webhook 自动触发 Jenkins Pipeline，并执行项目既有构建/部署流水线。

---

## 1. 整体架构

```text
开发者
  |
  | git push
  v
GitLab
  |
  | Push Event
  | Webhook
  v
Jenkins
  |
  | Pipeline script from SCM
  | deploy-pipline/Jenkinsfile
  v
构建流程
  |
  +--> Checkout
  +--> Maven package
  +--> Build backend images
  +--> Build frontend
  +--> Push images to Registry
  +--> Deploy
  v
Docker Desktop
```

当前 GitLab、Jenkins、Registry、Nginx 等 DevOps 服务运行在同一个 Docker 网络：

```text
peach-devops
```

因此容器之间应直接使用 Docker 服务名通信。

---

## 2. 当前关键地址

### 2.1 浏览器访问地址

Jenkins：

```text
http://localhost:8080
```

或：

```text
http://peachsoft.jenkins.test
```

GitLab：

```text
http://localhost:8929
```

或：

```text
http://peachsoft.gitlab.test
```

Registry：

```text
http://localhost:5000
```

### 2.2 Docker 容器内部地址

Jenkins 容器访问 GitLab：

```text
http://gitlab
```

GitLab 容器访问 Jenkins：

```text
http://jenkins:8080
```

**Webhook 中不要使用 `http://localhost:8080`。**  
对于 GitLab 容器而言，`localhost` 指向 GitLab 容器自身。

---

## 3. 验证 GitLab 与 Jenkins 网络

在仓库根目录执行：

```cmd
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins curl -I http://gitlab/
```

正常可看到：

```text
HTTP/1.1 302 Found
Location: http://gitlab/users/sign_in
```

这说明 Jenkins 已能访问 GitLab。

再执行：

```cmd
docker compose -f deploy-pipline/pipline/docker-compose.yml exec gitlab curl -I http://jenkins:8080/login
```

正常可看到：

```text
HTTP/1.1 200 OK
X-Jenkins: 2.x
```

这说明 GitLab 也能访问 Jenkins。

---

## 4. Docker Compose YAML 检查

如果 Compose 报：

```text
go-yaml load error in parser
```

先执行：

```cmd
docker compose -f deploy-pipline/pipline/docker-compose.yml config
```

如果本地 Compose 被误改：

```cmd
git diff -- deploy-pipline/pipline/docker-compose.yml
```

如果修改无需保留：

```cmd
git restore -- deploy-pipline/pipline/docker-compose.yml
```

---

# 5. Jenkins 凭据

## 5.1 GitLab 拉代码凭据

进入：

```text
Manage Jenkins
-> Credentials
-> System
-> Global credentials
-> Add Credentials
```

建议：

```text
Kind:
Username with password

ID:
gitlab-peach-cloud

Username:
GitLab 用户名

Password:
GitLab Personal Access Token
```

Token 至少要具备读取仓库所需权限，例如：

```text
read_repository
```

## 5.2 部署环境 Secret file

当前 Jenkinsfile 使用：

```text
peach-deploy-env
```

因此 Jenkins 中还需要：

```text
Kind:
Secret file

ID:
peach-deploy-env
```

文件可基于：

```text
deploy-pipline/peach-deploy.env.example
```

准备真实部署参数。

---

# 6. 创建 Jenkins Pipeline

进入 Jenkins：

```text
New Item
```

Job 名建议：

```text
peach-cloud-deploy
```

类型：

```text
Pipeline
```

---

# 7. 配置 Pipeline script from SCM

进入：

```text
peach-cloud-deploy
-> Configure
```

找到：

```text
Pipeline
```

设置：

```text
Definition:
Pipeline script from SCM

SCM:
Git
```

## 7.1 Repository URL

使用容器内部地址：

```text
http://gitlab/<group>/peach-cloud.git
```

例如：

```text
http://gitlab/peachsoft/peach-cloud.git
```

不要使用：

```text
http://localhost:8929/...
```

也不要使用：

```text
http://gitlab:8929/...
```

宿主机 `8929` 只是 Docker 端口映射，容器内部 GitLab HTTP 端口是 `80`。

## 7.2 Credentials

选择：

```text
gitlab-peach-cloud
```

## 7.3 Branch Specifier

部署分支为 `main` 时：

```text
*/main
```

注意：

```text
SCM Branch Specifier:
*/main
```

而 Webhook Allowed branches 使用：

```text
main
```

## 7.4 Script Path

填写：

```text
deploy-pipline/Jenkinsfile
```

项目目录名就是 `deploy-pipline`，不要误写成 `deploy-pipeline`。

---

# 8. 当前 Jenkinsfile 流程

现有 Jenkinsfile 已经包含完整构建/部署：

```text
Checkout
Validate Jenkins credentials
Build CI image
Maven package
Build and push images
Build frontend
Deploy
```

因此：

```text
push main
-> Jenkins
-> 构建
-> 镜像
-> Registry
-> Deploy
```

---

# 9. Webhook 前先确认手动构建

先点击：

```text
Build Now
```

确认 Pipeline 能完整执行成功。

Webhook 只是负责自动触发现有 Job，不负责解决 Jenkinsfile 本身的构建问题。

---

# 10. Jenkins 开启 GitLab Webhook Trigger

进入：

```text
peach-cloud-deploy
-> Configure
-> Build Triggers
```

勾选：

```text
Build when a change is pushed to GitLab
```

当前需求只是 Push 自动构建，因此推荐：

```text
Push Events                         开启
Merge Request Events               关闭
Pipeline Events                    关闭
Comments                           关闭
```

---

# 11. Advanced 推荐设置

## Enable [ci-skip]

建议开启。

这样提交：

```bash
git commit -m "docs: update readme [ci-skip]"
```

可以跳过 Jenkins。

## Ignore WIP Merge Requests

保持开启即可。

## Set build description to build cause

建议开启。

## Build on successful pipeline events

建议关闭，避免 GitLab Pipeline 成功后再次触发 Jenkins。

## Merge Request Cancel 相关

保持关闭：

```text
Cancel pending merge request builds on update
Cancel running merge request builds on update
```

---

# 12. Allowed branches

如果只允许 `main` 自动构建/部署：

选择：

```text
Filter branches by name
```

Include：

```text
main
```

Exclude：

```text
留空
```

效果：

```text
git push origin main
-> 触发 Jenkins
```

而：

```text
git push origin feature/test
```

不会触发该 Job。

---

## 12.1 main 不匹配警告

如果 Jenkins 显示：

```text
Following patterns don't match any branch in source repository: main
```

先确认 GitLab 确实存在 `main`。

然后确认 SCM：

```text
Branch Specifier:
*/main
```

保存 Job，执行一次：

```text
Build Now
```

成功 Checkout 后，再回到 Allowed branches 填：

```text
main
```

要记住：

```text
SCM:
*/main

Webhook Include:
main
```

---

# 13. Jenkins Secret Token

进入：

```text
Build Triggers
-> Build when a change is pushed to GitLab
-> Advanced
```

找到：

```text
Secret Token
```

点击：

```text
Generate
```

复制生成的 Token。

后面 GitLab Webhook 的 `Secret token` 必须与这里完全一致。

---

# 14. GitLab Webhook URL

如果 Jenkins Job 名：

```text
peach-cloud-deploy
```

Webhook URL：

```text
http://jenkins:8080/project/peach-cloud-deploy
```

注意使用：

```text
/project/
```

不要使用：

```text
/job/peach-cloud-deploy/build
```

---

# 15. GitLab 放行 Jenkins 内网访问

这项不在项目级：

```text
Project
-> Settings
```

而在 GitLab 管理员后台。

使用 Administrator 登录后：

```text
Admin
-> Settings
-> Network
-> Outbound requests
```

推荐 allowlist：

```text
jenkins:8080
```

如果当前 GitLab 版本只有全局选项，可以启用：

```text
Allow requests to the local network
from webhooks and integrations
```

但这个范围更大。

---

# 16. GitLab 项目创建 Webhook

进入项目：

```text
peach-cloud
-> Settings
-> Webhooks
```

填写：

```text
URL:
http://jenkins:8080/project/peach-cloud-deploy
```

Secret Token：

```text
填写 Jenkins Generate 出来的 Token
```

Trigger：

```text
Push events
```

如果 GitLab 页面提供 Branch filter：

```text
main
```

---

# 17. 最终推荐配置

Jenkins：

```text
Build when a change is pushed to GitLab      开启
Push Events                                  开启
Enable [ci-skip]                             开启
Ignore WIP Merge Requests                    开启
Set build description to build cause         开启
Build on successful pipeline events          关闭
Merge Request Events                         关闭
```

Allowed branches：

```text
Filter branches by name

Include:
main

Exclude:
空
```

GitLab：

```text
Webhook URL:
http://jenkins:8080/project/peach-cloud-deploy

Secret Token:
与 Jenkins 完全一致

Trigger:
Push events
```

GitLab Admin：

```text
Outbound requests allowlist:
jenkins:8080
```

---

# 18. 测试 Webhook

GitLab Webhook 页面：

```text
Test
-> Push events
```

正常情况下 Jenkins 会立即生成新的 Build。

构建原因通常类似：

```text
Started by GitLab push
```

---

# 19. 实际 Push 测试

```cmd
git status
git add .
git commit -m "test: verify jenkins webhook"
git push origin main
```

此时无需再手动点击 `Build Now`。

---

# 20. 完整调用链

```text
Developer
   |
   | git push origin main
   v
GitLab
   |
   | Push Event
   v
Webhook
   |
   | POST
   v
http://jenkins:8080/project/peach-cloud-deploy
   |
   v
Jenkins GitLab Plugin
   |
   v
peach-cloud-deploy
   |
   v
Pipeline script from SCM
   |
   | clone
   v
http://gitlab/<group>/peach-cloud.git
   |
   v
deploy-pipline/Jenkinsfile
   |
   +-- Checkout
   +-- Maven package
   +-- Build backend image
   +-- Build frontend image
   +-- Push Registry
   +-- Deploy
   |
   v
Docker Desktop
```

---

# 21. 常见问题

## Webhook 报 Local network not allowed

进入：

```text
GitLab Admin
-> Settings
-> Network
-> Outbound requests
```

添加：

```text
jenkins:8080
```

## Webhook 返回 404

检查 URL：

```text
http://jenkins:8080/project/peach-cloud-deploy
```

并确认 Job 名完全一致。

## Webhook 返回 403

检查 GitLab 和 Jenkins 两边的 Secret Token 是否完全一致。

## Jenkins 手动构建正常，但 Webhook 不触发

依次检查：

```text
1. GitLab -> Jenkins 网络
2. GitLab Outbound requests
3. Webhook URL 是否使用 jenkins:8080
4. Jenkins Trigger 是否开启
5. Secret Token 是否一致
6. Push Events 是否开启
7. main 是否在 Allowed branches
```

---

# 22. 推荐分支策略

当前建议：

```text
feature/*
-> 不触发部署 Job

main
-> 自动构建
-> 自动部署
```

后续可以演进：

```text
feature/*
-> 编译 + 测试
-> 不部署

main
-> 编译 + 测试 + 镜像
-> 部署开发/测试环境

tag v1.x.x
-> 构建正式镜像
-> 发布正式版本
```

---

# 23. 当前 Peach Cloud 关键配置汇总

## Jenkins SCM

```text
Repository URL:
http://gitlab/<group>/peach-cloud.git

Credentials:
gitlab-peach-cloud

Branch Specifier:
*/main

Script Path:
deploy-pipline/Jenkinsfile
```

## Jenkins Webhook Trigger

```text
Build when a change is pushed to GitLab:
开启

Push Events:
开启

Allowed branches:
Filter branches by name

Include:
main
```

## GitLab Webhook

```text
URL:
http://jenkins:8080/project/peach-cloud-deploy

Secret Token:
与 Jenkins 一致

Trigger:
Push events

Branch:
main
```

## GitLab Admin

```text
Admin
-> Settings
-> Network
-> Outbound requests

Allow:
jenkins:8080
```

---

# 24. 最终验证命令

Compose：

```cmd
docker compose -f deploy-pipline/pipline/docker-compose.yml config
```

查看容器：

```cmd
docker compose -f deploy-pipline/pipline/docker-compose.yml ps
```

Jenkins -> GitLab：

```cmd
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins curl -I http://gitlab/
```

GitLab -> Jenkins：

```cmd
docker compose -f deploy-pipline/pipline/docker-compose.yml exec gitlab curl -I http://jenkins:8080/login
```

最终：

```cmd
git push origin main
```

---

# 25. 检查清单

- [ ] GitLab 与 Jenkins 在同一个 Docker 网络
- [ ] Jenkins 可访问 `http://gitlab/`
- [ ] GitLab 可访问 `http://jenkins:8080/login`
- [ ] Jenkins Credential `gitlab-peach-cloud` 已创建
- [ ] Jenkins Credential `peach-deploy-env` 已创建
- [ ] Jenkins Job 已创建
- [ ] Pipeline Definition 使用 `Pipeline script from SCM`
- [ ] Repository URL 使用 `http://gitlab/...`
- [ ] Branch Specifier 为 `*/main`
- [ ] Script Path 为 `deploy-pipline/Jenkinsfile`
- [ ] Jenkins 手动 Build 正常
- [ ] `Build when a change is pushed to GitLab` 已开启
- [ ] `Push Events` 已开启
- [ ] Allowed branches 只允许 `main`
- [ ] Jenkins Secret Token 已生成
- [ ] GitLab Outbound requests 已允许 `jenkins:8080`
- [ ] GitLab Webhook URL 使用 `/project/peach-cloud-deploy`
- [ ] GitLab Secret Token 与 Jenkins 一致
- [ ] GitLab Webhook Test Push Event 成功
- [ ] `git push origin main` 后 Jenkins 自动构建

---

## 结论

当前 Peach Cloud Webhook 核心链路：

```text
GitLab
http://gitlab
       |
       | Push Event
       v
Jenkins Webhook
http://jenkins:8080/project/peach-cloud-deploy
       |
       v
Jenkins Pipeline
deploy-pipline/Jenkinsfile
       |
       v
Build + Image + Deploy
```

必须区分浏览器地址与容器内部地址：

```text
浏览器访问 Jenkins:
http://localhost:8080

GitLab Webhook 调 Jenkins:
http://jenkins:8080
```

```text
浏览器访问 GitLab:
http://localhost:8929

Jenkins 拉取 GitLab:
http://gitlab
```

只要容器网络、Job Trigger、Secret Token、Allowed branches 和 GitLab Outbound requests 配置正确，`main` 分支每次 Push 后即可自动触发 Jenkins 构建与部署。
