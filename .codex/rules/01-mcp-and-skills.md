# MCP And Skills Selection

本规则用于约束 agent 在 `peach-cloud` 内如何选择 MCP 和 skills，避免乱用工具或跳过项目约定。

## MCP Selection

- 代码结构、模块职责、调用链、影响面：先用 `codegraph`
- 当前会话可能只暴露 `codegraph_files`、`codegraph_search`、`codegraph_context`、`codegraph_impact`、`codegraph_status` 等部分工具；只按实际可用工具执行，不假设所有 codegraph 能力都存在
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

- `.codex/skills/` 下的目录名、`SKILL.md` 的 `name` 和 `$skill` 调用名必须保持一致，当前统一使用 `using-peach-*`
- 写或刷新 README、模块说明、starter 接入文档：使用 `using-peach-readme-writer`
- 涉及 `peach-middleware/peach-rocket`、RocketMQ、`@MqEvent`、`@MqConsumer`：使用 `using-peach-rocket`
- 涉及 `peach-component/peach-storage`、`StorageTemplate`、provider：使用 `using-peach-storage`
- 涉及 `peach-component/peach-threadpool`、`ThreadPoolManager`、`@AsyncExecuted`：使用 `using-peach-threadpool`
- 编写、生成、重构或审查 REST、Entity、DAO/XML、Service、common 归属：使用 `using-peach-code-skeleton`

## Skill Constraints

- 命中 skill 场景时，先完整读取 `SKILL.md`
- `SKILL.md` 引用 `references/module-guide.md` 时，涉及配置、边界、SPI、示例、README，必须继续读参考文件
- 未在 skill 或源码中确认的配置项、默认值、扩展点，不得编造
- 当用户一次性点名一组同类文件时，例如多个 controller、service、DTO 或其配套层级，必须先把这组文件全部扫完再动手，不能只改其中一个就结束
- 当任务涉及“补齐既有模式”时，必须先在仓库里找一个可工作的同类范式作为参照，例如注解、校验分组、日志、Javadoc，再开始批量修改
- 当用户后续补充了更强约束时，例如“不要拆 AddDTO/UpdateDTO，改用分组校验”，后续实现必须以新约束为准，之前的方案不能继续沿用
