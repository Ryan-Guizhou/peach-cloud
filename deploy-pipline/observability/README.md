# Peach Cloud 可观测性部署

该目录提供 Docker Compose 单机部署所需的指标、Trace 和日志组件配置，适用于本地、测试及中小规模单节点环境。

## 组件与数据流

```text
Peach services -- /actuator/prometheus --> Prometheus ----┐
Peach services -- OTLP/HTTP --> OTel Collector --> Tempo --+--> Grafana
Peach JSON log files --> Grafana Alloy --> Loki -----------┘
```

| 组件 | 职责 | 默认镜像版本 |
| --- | --- | --- |
| Prometheus | 拉取应用和 Collector 指标，接收 Tempo span metrics | `v3.12.0` |
| OpenTelemetry Collector | 接收、批处理、重试并转发 OTLP Trace | `0.153.0` |
| Tempo | Trace 存储、检索、服务图和 span metrics | `2.10.7` |
| Loki | JSON 日志存储和检索 | `3.7.4` |
| Grafana Alloy | 读取各服务 `ALL_FILE` JSON 日志并发送到 Loki | `v1.18.0` |
| Grafana | 统一查询指标、日志和 Trace | `13.1.0` |

版本通过 `peach-deploy.env` 覆盖，不使用 `latest`。

## 启动方式

复制环境变量模板并修改所有 `change_me_*` 值，特别是 Grafana 管理员密码：

```bash
cp deploy-pipline/peach-deploy.env.example deploy-pipline/peach-deploy.env
docker compose \
  --env-file deploy-pipline/peach-deploy.env \
  -f deploy-pipline/docker-compose.deploy.yml \
  --profile observability \
  up -d prometheus tempo otel-collector loki alloy grafana
```

默认入口：

- Grafana：`http://localhost:3000`
- Prometheus：`http://127.0.0.1:9090`
- Loki、Tempo 和 Collector 仅在 `peach-cloud-runtime` Docker 网络中访问。

Grafana 会自动配置 Prometheus、Loki 和 Tempo 数据源，并建立日志到 Trace、Trace 到日志、Trace 到指标的关联。

## 服务侧要求

Compose 已向业务服务注入以下配置：

- 暴露 `health`、`info`、`prometheus` Actuator 端点；容器未直接发布管理端口到宿主机。
- OTLP HTTP Trace 地址为 `http://otel-collector:4318/v1/traces`。
- 默认采样率为 `0.1`，可使用 `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` 调整。
- `ALL_FILE` 必须保持 Logstash JSON 输出，Alloy 只读取该文件，避免重复采集 INFO/WARN/ERROR 分文件。

## 数据和容量边界

- Prometheus 默认保留 15 天，可通过 `PROMETHEUS_RETENTION` 修改。
- Tempo 和 Loki 默认保留 7 天。
- 所有数据使用 Docker named volume，停止容器不会删除数据。
- 当前是单节点模式，不提供跨节点高可用、对象存储、TLS 或多租户隔离。生产集群需要迁移到 Kubernetes/集群模式，并接入对象存储、认证、备份和容量告警。
- `traceId`、`spanId` 和 `requestId` 不作为 Loki 标签或 Prometheus 标签，避免高基数索引；查询日志时使用 JSON 解析和内容过滤。

## 验证

```bash
docker compose --env-file deploy-pipline/peach-deploy.env \
  -f deploy-pipline/docker-compose.deploy.yml config

curl http://127.0.0.1:9090/-/ready
curl http://localhost:3000/api/health
```

发送一次经过 Gateway 的业务请求后：

1. 在服务 JSON 日志中确认 `requestId`、`traceId`、`spanId`。
2. 在 Grafana Explore 的 Loki 数据源查询 `{service="peach-gateway"} | json`。
3. 点击日志中的 TraceID 跳转 Tempo。
4. 在 Prometheus 查询 `up{job="peach-cloud-services"}` 检查已选择服务的抓取状态。

未部署的可选服务会在 Prometheus Targets 页面显示 `DOWN`，不影响已选择服务。
