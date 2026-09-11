# Evidence First

## Evidence Matrix

写文档前内部建立最小证据矩阵：

| 文档事实 | 优先证据 |
| --- | --- |
| 模块/artifactId/依赖 | `pom.xml` / `package.json` |
| 公共 API / SPI | interface、public type、实现与测试 |
| 配置 key / 默认值 | `@ConfigurationProperties`、metadata、配置类 |
| 自动装配 | AutoConfiguration、imports、Bean 注册逻辑 |
| 调用链 / 影响面 | 当前源码 + 必要时 CodeGraph |
| 数据结构 | Entity、DDL、Mapper/XML、迁移脚本 |
| 运行行为 | 实现代码 + 测试 + quickstart |
| 第三方行为 | 当前版本官方资料 / Context7 |

证据不足时继续取证或降低结论强度。不要先写结论再寻找证明。

## 禁止推断为事实

- “类名看起来像支持”不等于已支持。
- “存在接口”不等于有生产级实现。
- “有内存实现”不等于持久化/集群可靠。
- “发送成功”不等于业务最终一致。
- “注解存在”不等于所有调用路径都经过切面。
- “README 写过”不等于当前代码仍然支持。
