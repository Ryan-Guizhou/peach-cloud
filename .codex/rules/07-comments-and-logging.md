# Comments And Logging

本规则用于约束 `peach-cloud` 项目中的注释、Javadoc 和日志风格。

## Javadoc Scope

- 类注释、方法注释统一使用 Javadoc 风格
- 新增类、接口、枚举、非显而易见的方法时，按项目既有习惯补充注释
- 注释内容要描述真实职责，不写空话，不复制模板废话
- REST Controller 的类注释必须说明所属业务域、职责边界和主要接口用途，不能只写一句泛化描述
- 如果同一批 controller 需要补 Javadoc、校验注解和操作日志注解，应当一起补齐，避免只完成部分文件
- 新增 Java 类时，必须逐个检查并补齐类注释标签，不能只写简短中文说明后就结束
- REST / Service / DAO / Entity 这类新增核心类，类注释至少要说明业务域、职责边界和模块归属，避免出现“XXX实现”这种过于空泛的描述
- 如果新增文件里存在乱码、半角符号错位或历史脏注释，优先在当前变更中一并修正，不要把注释质量问题留给后续

## Required Javadoc Tags

- 类注释必须包含以下标签：
  - `@Author Mr Shu`
  - `@Version 1.0.0`
  - `@CreateTime yyyy/M/d HH:mm`
- `@CreateTime` 必须按当前系统时间生成，不能随意猜测或复用旧时间
- 方法注释如为自定义方法，直接在方法上补充 Javadoc
- 方法注释如为接口定义的方法，优先在接口定义处补充；实现类尽量不重复复制同一份方法注释

## Method Comment Rules

- 自定义方法应说明用途、关键参数、返回值和必要的异常语义
- 接口方法的实现类若语义未变化，不重复写一份几乎相同的注释
- 如果实现类相对接口增加了额外行为、边界或副作用，可以在实现类补充必要说明

## Field Comment Rules

- DO、DTO、QO、VO 不强制添加属性级 Javadoc 注释
- 这类模型属性的说明统一优先使用 Swagger 注解，例如 `@Schema(description = "...")`
- 如某个属性存在额外边界、格式限制或易误用语义，优先在类注释或相关方法注释中补充，不在每个字段上重复堆叠 Javadoc

## Logging Rules

- 日志统一使用 Lombok 的 `@Slf4j`
- 按语义选择日志级别：调试信息用 `debug`，常规关键流程用 `info`，可恢复异常或预警用 `warn`，错误和失败用 `error`
- 不使用 `System.out.println`、`printStackTrace`
- 日志内容要能定位问题，避免只打印“执行失败”这类无上下文信息
- 日志中禁止输出密钥、token、密码、签名 URL、完整敏感报文
