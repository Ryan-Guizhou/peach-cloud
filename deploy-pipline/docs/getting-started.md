# Peach Cloud Docker 启动与迁移

本文区分 **已经跑通的现有环境** 与 **全新主机冷启动**。现有环境优先保护数据，不需要重新执行一串初始化脚本。

## 1. 已有 Jenkins/GitLab 环境：推荐路径

只维护 Jenkins Secret file `peach-deploy-env`，模板为 `deploy-pipline/env/deploy.env.example`。必填主要是 Docker Host 可见目录和密码/凭据；Mongo URI、组件版本、端口、Namespace、Group 等由 [`env/defaults.env`](../env/defaults.env) 和渲染脚本提供，旧版完整 Secret file 仍可继续使用。

日常操作只有两种：GitLab push/webhook 自动触发 Jenkins，或 Jenkins `Build with Parameters` 指定分支/构建模式/服务。

```text
Secret + defaults
      ↓
只读检查已有 Jenkins/Nexus/Registry
      ↓
Runtime Reconcile（不重建 DevOps）
      ↓
MySQL / Mongo User / Nacos / RocketMQ Topic 幂等初始化
      ↓
按需 Maven / Frontend build
      ↓
镜像 push / 指定服务部署 / health check
      ↓
版本与兼容性报告
```

## 2. 数据保护保证

已存在 `gitlab`、`jenkins`、`nexus`、`local-registry`、`registry-ui`、`peach-devops-nginx` 时，Jenkins 不通过 DevOps Compose 重建它们。对应 `peach-gitlab-*`、`peach-jenkins-data`、`peach-nexus-data`、`peach-registry-data` external volume 保持原身份。

运行时容器采用 reconcile：存在则保留，停止则 `docker start`，只有不存在才根据 Compose 创建。普通自动化禁止删除 volume。

## 3. Jenkins 参数

| 参数 | 作用 |
| --- | --- |
| `BUILD_BRANCH` | `AUTO` 使用当前 SCM/Webhook；也可填写具体远程分支 |
| `BUILD_MODE` | `AUTO` / `SELECTED` / `ALL` |
| `DEPLOY_SERVICES` | `SELECTED` 模式下空格或逗号分隔服务 |
| `STOP_UNSELECTED_SERVICES` | 默认 `false`，未选服务保持运行 |

`AUTO` 根据 Git diff 和 [`config/services.json`](../config/services.json) 计算影响范围。公共后端模块变化会构建全部后端；部署体系变化会安全回退为全部应用构建。

## 4. RocketMQ 自动治理

默认 `ROCKETMQ_TOPIC_PROVISION_MODE=ensure`。Topic 声明位于 `config/rocketmq/topics.json`，当前治理静态 Topic `scheduler-execution-result`。`scheduler-execute-{applicationName}` 属于动态应用 Topic；为了保持原版本行为，仍保留 Broker/Starter 自动创建能力，并在报告中明确展示。

Jenkins Archive 可查看：

```text
runtime/reports/rocketmq-status.txt
runtime/reports/compatibility-report.md
```

## 5. 全新主机冷启动

只有主机尚未存在 Jenkins 时才需要人工执行一次：

```bash
cp deploy-pipline/env/deploy.env.example deploy-pipline/env/deploy.env
# 填写私有值
PEACH_ENV_FILE=deploy-pipline/env/deploy.env deploy-pipline/scripts/bootstrap/bootstrap.sh
```

Bootstrap 会先合成 runtime env，再盘点/创建缺失网络和 volume、同步宿主机可见配置、启动缺失基础设施并执行幂等初始化。已有同名容器和 volume 优先复用。

## 6. 禁止的日常操作

```text
docker compose down -v
docker volume prune
docker system prune --volumes
docker volume rm <protected-volume>
```

排障优先查看 `docker ps -a`、`docker logs`、network/volume inspect 和 Jenkins Archive 报告，不通过删除数据重建来修复。
