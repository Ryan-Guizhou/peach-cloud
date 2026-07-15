# Comments And Logging

## Comments

- 类、接口和非显而易见的公开方法使用 Javadoc，说明真实业务职责、边界、参数、返回值和必要异常，不重复代码本身。
- Service 接口声明公开能力；实现类语义未变化时不复制同一份方法注释。
- 模型字段优先使用 `@Schema(description = "...")`，特殊边界再补 Javadoc。
- `@Author`、`@Version`、`@CreateTime` 属于 `LEGACY_COMPATIBLE`：维护已有统一模板时可沿用，但不得伪造实际作者或为了形式生成无价值时间戳；新模块是否保留由用户或模块规范决定。
- 禁止乱码、占位注释、与实现不一致的长篇说明和逐行翻译式注释。

## Runtime Logging

- 仅实际记录日志的类添加 `@Slf4j`；禁止 `System.out.println` 和 `printStackTrace`。
- `debug` 用于调试细节，`info` 用于关键正常流程，`warn` 用于可恢复异常或风险，`error` 用于执行失败；操作类型本身不决定日志级别。
- 日志包含必要业务标识、阶段、结果和异常上下文，避免只写“执行失败”。
- 异常日志使用参数化消息并传入异常对象；不得重复打印同一异常链。

## Audit And Sensitive Data

`FORBIDDEN`：

- 输出密码、token、secret、私钥、签名 URL、身份证号、完整请求/响应或消息体。
- 在 `@UserOperLog` 中使用 `#p0` 等表达式记录完整 DTO；DTO 即使当前没有敏感字段，也不应形成可被后续字段扩展破坏的隐式白名单。
- 复制存量代码中 DELETE=`ERROR`、UPDATE=`DEBUG` 之类机械级别映射。

操作审计应显式选择稳定且非敏感的字段，例如业务 ID、操作类型、资源类型和执行结果。新增字段后必须重新检查 DTO `toString()`、SpEL 和序列化链路。
