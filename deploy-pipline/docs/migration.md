# 从现有 deploy-pipline 迁移

本次迁移是 **配置与目录迁移，不是数据迁移**。

## 保持不变

- 现有 Container 名称。
- `peach-devops`、`peach-cloud-runtime` 网络名称。
- GitLab/Jenkins/Nexus/Registry/MySQL/Redis/Nacos/RocketMQ/Observability 的 named volume 名称。
- Jenkins credential ID `peach-deploy-env`。

MongoDB 新增 `peach-mongo` 与 `peach-mongo-data`。

## 步骤

1. 运行 `scripts/bootstrap/inspect-existing.sh` 保存当前资源清单。
2. 备份重要 Volume，尤其 GitLab/Jenkins/Nexus/MySQL。
3. 基于 `env/deploy.env.example` 更新 Jenkins Secret file；新日志根目录为 `deploy-pipline/runtime/logs`。
4. 运行 `bootstrap.sh`。已有受保护容器不会被 force recreate，停止容器只会 start。
5. 确认 Middleware 初始化和验证完成后，再让 Jenkins 使用新 `Jenkinsfile`。
6. 旧目录删除只影响仓库配置文件，不删除 Docker named volume。

不要执行 `docker compose down -v` 或 volume prune 作为迁移步骤。
