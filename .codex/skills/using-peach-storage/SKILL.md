---
name: using-peach-storage
description: 修改或审查 peach-component/peach-storage 的 StorageTemplate、StorageProvider/Factory、对象路径、Provider 配置、能力矩阵、前端直传、分片上传、多 Provider 路由、资源关闭和存储一致性边界时使用。
---

# Peach Storage

## 核心不变量

- 业务优先通过 `StorageTemplate` 和现有统一契约访问存储，不在业务层散落厂商 SDK。
- `objectKey` 表示业务对象 key，统一 `/` 语义；拒绝本地绝对路径、URL、`..` 越界和不可信路径逃逸。
- `StorageProviderFactory` 负责类型匹配、启动期校验与 Provider 创建；`StorageProvider` 负责运行期能力和资源生命周期。
- Provider 能力通过 `StorageCapability`/`capabilities()` 明示；业务在直传、分片、大文件等场景先检查能力，不依靠异常猜测。
- SDK client、连接池、线程等资源必须有明确关闭路径。
- `copy`、`move`、批量删除、对象存储 + DB 组合操作不宣称分布式事务；必须设计部分成功、重试、幂等或补偿边界。
- 大对象优先流式/分片/直传，不对无上限输入整体读入内存。
- secret、签名 URL、上传 token 不进入日志和正式文档。

新增 Provider 同时检查 `StorageType`、Provider、Factory、SPI/自动配置、配置校验和测试。涉及当前模块的具体配置或 capability 时按需读取现有 `references/module-guide.md`。

需要文档时进入 `project-doc-engineer`。
