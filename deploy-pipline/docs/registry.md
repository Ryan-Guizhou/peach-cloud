# Docker Registry

Registry 容器身份继续使用 `local-registry`，数据 Volume 继续使用 `peach-registry-data`。Jenkins 构建镜像后推送 `localhost:5000/peach-cloud/<service>:<12-char-git-sha>`。

Application Compose 只负责 pull/run，不在部署阶段重新 build。业务流水线不负责清理历史镜像或 Registry Volume。
