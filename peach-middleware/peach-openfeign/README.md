# peach-openfeign

[English](README.en-US.md) | 中文

`peach-openfeign` 是 Peach Cloud 的服务间 HTTP 调用治理 Starter，统一 Same-Token、RequestId、超时、有限重试、Sentinel 与异常边界。业务 Feign 契约仍由各业务 `*-openfeign-external` 模块维护。

## 结构

| 模块 | 职责 |
| --- | --- |
| `peach-openfeign-autoconfigure` | 拦截器、超时/重试、异常治理、Sentinel 和自动配置 |
| `peach-openfeign-starter` | 业务接入依赖入口 |
| `peach-openfeign-quickstart` | 本机 Stub + Feign Client 闭环，验证治理自动装配 |

```mermaid
flowchart LR
    Service[调用服务] --> Starter[peach-openfeign-starter]
    Starter --> Auto[peach-openfeign-autoconfigure]
    Contract[*-openfeign-external] --> Feign[Feign Client]
    Auto --> Feign
    Feign --> Remote[目标服务]
```

## Quick Start

示例代码位于 [`peach-openfeign-quickstart`](./peach-openfeign-quickstart/)。真实 Feign 契约仍应来自业务 `*-openfeign-external` 模块，QuickStart 不复制业务接口，也不依赖外部真实服务。

| 项 | 说明 |
| --- | --- |
| 能力样例 | 本机 `DownstreamStubController` + `LocalStubClient` 闭环 |
| 成功调用 | Feign `GET /stub/echo` 回显 `source=stub` |
| 错误分类 | Stub 返回 500，`PeachOpenFeignErrorDecoder` 转为 `PeachFeignRemoteException` |
| 超时分类 | Stub 返回 408，ErrorDecoder 转为 `PeachFeignTimeoutException` |
| Runner | `OpenFeignDemoRunner`；测试设 `quickstart.openfeign.demo.enabled=false` |
| 前置 | 已关闭 Same-Token、Sentinel、fallback 启动强校验与重试，以便直接观察 ErrorDecoder 分类 |
| 端口 | 演示运行默认 `18085`（Stub 与调用方同进程）；测试使用 `RANDOM_PORT` + `local.server.port` |

```bash
mvn -pl peach-middleware/peach-openfeign/peach-openfeign-quickstart -am spring-boot:run
mvn -pl peach-middleware/peach-openfeign/peach-openfeign-quickstart -am test
```

## 边界

- 仅传播项目约定的 Same-Token 与 RequestId，不把任意业务 Header 变成默认透传。
- 重试只用于明确可恢复且可安全重试的调用；写请求不能默认盲目重试。
- Sentinel 负责调用治理，但业务 fallback 数据语义由调用方决定。
- 大文件传输优先使用对象存储等专用通道，不依赖 Feign 作为无限中转层。
