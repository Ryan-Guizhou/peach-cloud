# Language And Encoding

本规则用于限制语言级别、JDK API 选择和中文文件编码，避免 agent 生成不兼容或乱码内容。

## Java Compatibility

- 所有 Java 代码必须兼容 Java 8
- 禁止使用 Java 9+ 语法：`var`、`record`、`sealed`、text block、switch expression、pattern matching
- 禁止使用 Java 9+ 常见 API：`List.of`、`Set.of`、`Map.of`、`Stream.toList`、`Optional.isEmpty`、`Files.readString`、`Files.writeString`、`Path.of`
- 日期时间方案优先沿用当前模块已有写法；如使用 `java.time`，必须确认当前模块和上下游已一致采用

## Framework Compatibility

- Spring Boot 代码和配置按 `2.7.13` 能力边界编写
- Spring Cloud 按 `2021.0.5` 能力边界编写
- Spring Cloud Alibaba 按 `2021.0.5.0` 能力边界编写
- 不确定某个框架 API、配置项、注解行为时，先查 `context7` 或仓库现有实现

## Encoding Rules

- 所有源码、Markdown、YAML、properties、XML 文档保持 UTF-8
- 中文内容必须可正常显示，禁止提交乱码或 mojibake 文本
- 终端显示乱码时，先区分控制台编码问题和文件实际损坏，再决定是否修复文件
- 修改中文文件后，必须自检是否出现异常拉丁字符组合、Unicode replacement character（U+FFFD）等典型 mojibake 片段
