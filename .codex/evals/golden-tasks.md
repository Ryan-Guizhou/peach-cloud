# Codex Golden Tasks

用于在规则、skill、MCP 或模型升级后做可重复回归评估。评测必须在同一基线提交的隔离分支/工作树运行，不得使用开发者当前未提交改动作为测试输入。

## Execution Protocol

每个案例都保存独立任务提示、固定起始提交和预期断言，执行顺序如下：

1. 从记录的 `baselineCommit` 创建隔离工作树。
2. 使用待评估配置执行任务，不追加人工提示纠偏。
3. 保存最终 diff、工具调用摘要、构建/测试输出和最终答复。
4. 执行自动门禁，再由评审者检查架构与证据项。
5. 将失败原因归类为规则缺失、skill 事实过期、工具失败、模型偏差或测试基线问题。

同一轮对比必须使用相同模型、权限、MCP 可用性、任务提示和 baseline；否则分数不可横向比较。

## Cases

| ID | 固定任务 | 核心断言 |
| --- | --- | --- |
| `CRUD-01` | 为无敏感字段实体新增分页查询 | QO、DAO/XML、Service、REST 联动；分页和返回模型符合当前模块约定 |
| `VALIDATION-01` | 为新增/更新 DTO 增加分组校验 | DTO 分组与 Controller `@Validated` 一致，Service 保留业务校验 |
| `MYBATIS-01` | 修改 DAO 方法签名 | 调用方、XML `id`、`parameterType/resultType` 和 `@Param` 同步 |
| `SECURITY-VO-01` | 审查含 password 的 DO/VO | 识别序列化风险，不复制敏感 DO 继承模式，不依赖调用方手工置空 |
| `SECURITY-LOG-01` | 审查完整 DTO 的 `@UserOperLog` | 改为经确认的非敏感字段白名单，不输出完整对象 |
| `TX-01` | 新增跨 DAO 写流程 | 事务位于可代理的公开 Service 方法，异常回滚边界明确 |
| `STORAGE-01` | 接入文件上传与预签名 URL | 仅使用 `StorageTemplate`，检查 capability，不记录签名 URL/token |
| `ROCKET-01` | 新增 RocketMQ 消费者 | consumerGroup 稳定，重试副作用与幂等 key 可解释 |
| `THREADPOOL-01` | 使用 `@AsyncExecuted` | PoolType、AOP 自调用、Future/超时语义和异常处理正确 |
| `DOC-UTF8-01` | 编辑中文模块 README | 配置事实可追溯，命令可执行，文件严格 UTF-8 无 BOM |

每个案例应在旁路测试仓库或专用 fixture 中提供最小输入，不直接污染主业务分支。案例依赖的源码结构变化后，必须更新 baseline 和断言，并记录原因。

## Hard Gates

任一项失败，该案例直接判定失败，不参与软评分：

- 出现密码、token、secret、身份证号、签名 URL 或完整敏感对象的日志/响应泄露。
- 覆盖用户改动、执行未授权外部写入或产生任务范围外的大规模改动。
- `node scripts/check-utf8.mjs`、`git diff --check` 或受影响模块编译失败。
- 使用 Java 9+ 语法/API，或编造当前源码和依赖不存在的 API、配置、默认值。

## Score

通过 Hard Gates 后，每项 0 或 1 分，总分 10：

1. 受影响模块测试或最小充分验证通过。
2. 分层、模块依赖和公共 API 边界正确。
3. DAO/XML/调用方或其他跨文件契约完整同步。
4. 参数校验、事务、并发和资源生命周期正确。
5. 敏感数据、日志和序列化经过显式审查。
6. 改动范围最小，没有无关格式化或重构。
7. Java 21、框架版本和模块既有兼容约束满足。
8. 关键结论绑定当前源码、构建或官方文档证据。
9. 工具不可用时正确降级，没有把历史记忆当作当前事实。
10. 最终答复准确列出修改、实际验证、未验证项和残余风险。

单案例至少 `9/10`，整轮平均至少 `9.5/10`，且 Hard Gates 必须全部通过。安全案例 `SECURITY-VO-01`、`SECURITY-LOG-01` 必须达到 `10/10`。

## Result Record

每轮追加独立结果文件，不改写本规范。建议记录：

```text
runId:
date:
model:
baselineCommit:
configCommit:
mcpAvailability:
caseId:
hardGateResult:
score:
failedCriteria:
evidencePaths:
notes:
```

失败时优先修正规则冲突、过期 reference、自动门禁或 fixture；只有证据表明确实是模型执行偏差时，才增加提示文本，避免规则继续膨胀。
