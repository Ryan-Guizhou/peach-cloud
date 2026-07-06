# MCP And Skills Selection

本规则用于约束 agent 在 `peach-cloud` 内如何选择 MCP 和 skills，避免乱用工具或跳过项目约定。

## MCP Selection

- 代码结构、模块职责、调用链、影响面：先用 `codegraph`
- README、SQL、脚本、配置、纯文本：`codegraph` 不适合时再用 `rg` / 直接读文件
- 第三方库、框架、SDK、CLI 当前文档：用 `context7`
- GitHub 仓库、PR、issue、CI：只有用户明确要求时用 `github`
- 数据库连接、schema、表结构、只读 SQL：只有用户要求并且环境具备时用 `mysql`
- IntelliJ 当前项目模块、打开文件、符号快速文档、数据库连接、格式化：用 `idea`

## MCP Constraints

- 不要用 `context7` 搜项目源码
- 不要用 `rg` 重建 `codegraph` 已经能直接回答的问题
- 不要假设 `idea` 一定在仓库配置中声明；只有当前会话可用时才使用
- `mysql` 默认只读；没有明确授权不做写操作
- 涉及 secrets 的返回值不写入文件，不原样回显

## Skills Selection

- 写或刷新 README、模块说明、starter 接入文档：使用 `peach-readme-writer`
- 涉及 `peach-middleware/peach-rocket`、RocketMQ、`@MqEvent`、`@MqConsumer`：使用 `peach-rocket-starter`
- 涉及 `peach-component/peach-storage`、`StorageTemplate`、provider：使用 `peach-storage-starter`
- 涉及 `peach-component/peach-threadpool`、`ThreadPoolManager`、`@AsyncExecuted`：使用 `peach-threadpool`

## Skill Constraints

- 命中 skill 场景时，先完整读取 `SKILL.md`
- `SKILL.md` 引用 `references/module-guide.md` 时，涉及配置、边界、SPI、示例、README，必须继续读参考文件
- 未在 skill 或源码中确认的配置项、默认值、扩展点，不得编造
