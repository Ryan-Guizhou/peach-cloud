# Evidence First

## Evidence Matrix

非简单文案类文档在正式编写前，必须先在工作上下文中建立最小 Evidence Matrix。矩阵不要求提交到仓库，但每一个会影响使用方式、兼容性、配置、API、运行行为或故障处理的结论都必须能追溯到当前证据。

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

最小记录格式：

```text
Fact -> Evidence -> Confidence/Status
```

例如：

```text
VirtualExecutorService supports submit(Callable)
-> api/VirtualExecutorService.java + quickstart/VirtualThreadScenarioService.java
-> Supported
```

## 证据闭环

1. 先列文档必须回答的问题，再找证据，不先写结论。
2. 配置表逐项对照配置类/metadata；API 表逐项对照 public contract；示例优先复用 quickstart/test。
3. 涉及并发、事务、取消、重试、幂等、生命周期等行为时，必须至少读取实现代码；只有接口或 README 不足以证明运行语义。
4. 涉及第三方框架行为时先确认项目实际版本，再读取对应版本官方资料；通用记忆不能替代版本证据。
5. 证据不足时继续取证、删除结论，或将状态降为 `Experimental / Planned / Unconfirmed`，禁止补全式猜测。

## 禁止推断为事实

- “类名看起来像支持”不等于已支持。
- “存在接口”不等于有生产级实现。
- “有内存实现”不等于持久化/集群可靠。
- “发送成功”不等于业务最终一致。
- “注解存在”不等于所有调用路径都经过切面。
- “README 写过”不等于当前代码仍然支持。
- “相邻 Starter 这样做”不等于当前模块也这样做。

## 完稿前反向核验

文档完成后，从最终正文反向抽查所有高风险事实：配置 key/default、公共 API、状态/异常、并发边界、生命周期、命令、相对链接。任何无法重新定位到证据的内容必须删除或降级表述。
