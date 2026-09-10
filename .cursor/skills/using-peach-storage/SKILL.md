---
name: using-peach-storage
description: 规范 peach-cloud 项目中 peach-store-starter / peach-store-autoconfigure 的统一存储接入、StorageTemplate 调用、provider 配置、请求响应对象、路径安全、日志脱敏、分片上传、前端直传、多 provider 路由和新存储 Provider 扩展。Use when editing storage code, configuring peach.storage, adding StorageProvider/StorageProviderFactory, or writing README for peach-component/peach-storage.
---

# Peach Storage Starter

## 工作流

1. 先确认改动目标是业务接入、provider 扩展、配置调整、示例补充还是 README 更新。
2. 新通用业务接入优先通过 `StorageTemplate`；当前业务已使用 `MultiZoneStorage` 时先核实多区域语义和调用链，不为对齐名字直接替换。禁止业务散落调用厂商 SDK。
3. provider 扩展遵循 `StorageProviderFactory` 负责启动期校验和创建，`StorageProvider` 负责运行期读写。
4. 涉及配置、能力矩阵、路径语义或扩展细节时，读取 `references/module-guide.md`。
5. 改动后运行 `node scripts/check-utf8.mjs`、`mvn -f "peach-component/peach-storage/pom.xml" test`（或更小范围测试）和 `git diff --check`。

## 使用规则

- 引入依赖时使用 `com.peach:peach-store-starter`。
- 配置必须包含 `peach.storage.primary`，且命中 `peach.storage.providers` 中的一个 key。
- `objectKey` 只能表示业务对象 key，统一使用 `/`，不要传本地绝对路径、URL 或包含 `..` 的越界路径。
- 上传、下载、删除、复制、移动、列表、元信息、预签名 URL、前端直传和分片上传都优先使用请求对象。
- 指定非默认 provider 时使用 `storageTemplate.upload("archive", request)` 这类命名 provider 方法。
- `copy`、`move`、`batchDelete` 不承诺分布式事务；README 和业务代码都必须暴露部分成功或补偿边界。
- 密钥、签名 URL、前端直传 token 不允许完整写入日志。
- 大文件、前端直传、分片上传前先确认 provider 是否声明对应 `StorageCapability`。

## Provider 扩展规则

- 新增存储类型时同时考虑 `StorageType`、`StorageProvider`、`StorageProviderFactory`、SPI 注册文件和测试。
- factory 的 `validate` 负责必填项、SDK 依赖和类型专属约束；不要把启动期校验散落在运行期方法中。
- provider 持有 SDK client、连接池或线程资源时必须实现关闭逻辑，并确保 registry 能统一释放。
- 能力差异通过 `capabilities()` 显式声明，不要让业务靠异常猜测能力。

## 代码审查重点

- 检查是否绕过统一存储契约直接调用厂商 SDK；MultiZoneStorage 等现有适配必须核对真实职责。
- 检查 `root-path`、`prefix`、`bucket-name` 的语义是否混用。
- 检查删除和批量删除调用方是否可信，是否有审计或确认来源。
- 检查 `InputStream` 是否由调用方正确关闭。
- 检查 provider 配置是否把 secret 写死在仓库中。
- 检查新增 provider 是否有最小单元测试和配置校验测试。

## README 提醒

用户要求文档或公共 API、配置、扩展点、运行机制、生产边界变化时，使用 `$using-peach-readme-writer` 更新受影响 README；纯内部等价重构不强制刷新。README 必须明确支持的 provider、能力矩阵、配置示例、路径安全边界、前端直传/分片限制和扩展方式。

## 装配与一致性验收

- 修改 starter 装配/依赖时读取基础骨架的 `references/starter.md`，按当前客户端 `10-architecture-and-starters` 检查可选 SDK 和资源所有权。
- 文件大小、并发数与内存预算决定流式/临时文件/直传方案，不对无上限输入直接全量读入 byte[]。
- 上传与数据库组合时验证外部成功/落库失败、重复提交和恢复；清理必须验证对象所有权，不能删除其他请求复用的对象。
- capability、开关、缺失可选 SDK、自定义 provider、初始化失败及关闭场景按本次变化补测试。
