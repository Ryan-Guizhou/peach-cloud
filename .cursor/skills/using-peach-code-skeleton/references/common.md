# Common Placement

用于判断复用代码应留在业务模块、模块 `*-common`，还是进入全局 `peach-common`。安全、依赖方向和真实复用高于存量命名习惯。

## 导航结构

```text
peach-cloud/
├── peach-common/                         # 全局、无业务域语义的基础能力
│   └── src/main/java/com/peach/common/
├── peach-auth/
│   └── peach-auth-common/                # 认证域内部共享
├── peach-message/
│   └── peach-message-common/             # 消息域内部共享
└── peach-generator/
    └── peach-generator-common/           # 生成器域内部共享
```

先定位所属业务域，再决定是否上提；不要从“未来可能复用”反推全局 common。

## REQUIRED

- 只在一个业务域复用的常量、枚举、转换器和工具放该域的 `*-common`。
- 进入 `peach-common` 的代码不得依赖任何业务 DTO/DO/VO/QO，不得携带表、流程或状态机语义。
- 上提前确认至少有多个模块的稳定真实调用，并检查不会形成反向依赖或循环依赖。
- 敏感数据处理工具必须默认安全，例如脱敏而不是明文格式化；不得把业务密钥和生产配置做成公共常量。

## PREFERRED

- 闭合集合使用 enum；无状态工具使用 `final class` + 私有构造；配置使用类型安全对象。
- 复用接口保持最小，避免把仅为一个调用方服务的大型 helper 暴露成公共 API。
- 模块 common 只暴露稳定语义，业务流程仍留在 Service。

## LEGACY_COMPATIBLE

- `interface XxxConst`、历史 `common/comon` 包名仅在维护既有引用时兼容，不作为新模块模板。
- 修正历史包名会影响大量调用时，先评估迁移范围；不得同时新增两套平行包结构。

## FORBIDDEN

- 将缓存 key、业务状态码、Topic、表字段或领域异常直接塞入 `peach-common`。
- 为消除少量重复提前创建全局工具。
- 通过公共静态可变字段保存请求、用户、线程或业务流程状态。
- 为保持存量风格继续新增拼写错误的包名或类型名。

## 决策检查

1. 是否含业务域、表或流程语义？是：留在业务域。
2. 是否已有多个模块稳定调用？否：不要上提。
3. 是否依赖业务模型或上层模块？是：禁止进入 `peach-common`。
4. 是否会扩大公共 API 或安全暴露面？是：先做影响分析。
5. 修改后运行受影响模块测试、UTF-8 检查和 `git diff --check`。
