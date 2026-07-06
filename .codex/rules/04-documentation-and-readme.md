# Documentation And README

本规则用于限制 agent 产出的文档质量，尤其是 README 和模块说明。

## Before Writing

- 先读模块 `pom.xml`
- 再读源码入口、配置类、自动配置、示例代码、已有 README
- 需要写 starter 文档时，同时确认默认实现、覆盖方式、边界和验证方法

## Required Content

- 中文文档用 `README.md`
- 英文文档用 `README.en-US.md`
- 模块定位必须说明“解决什么”和“不解决什么”
- 配置说明必须只写仓库中可确认的配置项
- 验证部分必须给出可以执行的 Maven 或 npm 命令
- 排障部分要给出现象、检查点、处理方式

## Prohibited Content

- 不复制乱码和历史遗留脏内容
- 不把 `target/`、`.flattened-pom.xml`、日志目录、IDE 目录写成源码结构
- 不承诺源码中不存在的能力
- 不在未确认时编写默认值；不确定就明确写“当前未在配置类中声明默认值”
- 不暴露真实密钥、token、数据库密码、签名 URL
