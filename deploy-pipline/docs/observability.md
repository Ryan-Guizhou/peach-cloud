# 可观测性

Observability Compose 独立运行 Prometheus、Tempo、OpenTelemetry Collector、Loki、Alloy 和 Grafana，并继续复用现有 `peach-*-data` volumes 与配置文件。

业务服务通过 `otel-collector:4318` 上报 Trace；Prometheus 加入 Runtime 网络抓取 Actuator 指标；Alloy 读取 `runtime/logs/application/*` 并发送 Loki。Grafana 继续聚合 Metrics、Logs 和 Traces。
