---
name: using-peach-code-skeleton
description: 约束 peach-cloud 项目代码骨架与分层写法。Use when writing, generating, refactoring, or reviewing peach-cloud code, especially for REST controllers, entity/DO/DTO/QO/VO models, MyBatis DAO and DAO XML, service interfaces/implementations, or deciding whether shared constants/utilities belong in module common or peach-common. Treat this skill as the first-priority coding rule before applying module-specific skills.
---

# Peach Code Skeleton

按以下顺序工作：

1. 先判断本次改动落在哪一层：`rest`、`entity`、`dao.xml`、`service`、`common`。
2. 只读取本次任务相关的参考文件，不要把所有层的细节都塞进上下文。
3. 先区分当前模块属于哪种现有风格再编码，例如 `internal/external controller`、`PageInfo/PageResult`、`DO/VO/QO/DTO`、`interface Const`。
4. 如果现有实现与本 skill、仓库规则、用户要求冲突，优先级按“用户最新要求 > 仓库规则 > 本 skill > 历史存量代码”处理，并明确说明取舍。

强制要求：

- 把本 skill 视为 `peach-cloud` 编码第一准则，尤其在新建文件、批量补注解、DAO XML 调整、Service 抽象时先执行。
- 本 skill 的目标是“拟合当前仓库真实风格”，不是输出教科书式 Spring/MyBatis 模板；优先模仿现有模块的命名、返回模型、注解组合和分层方式。
- 涉及外部库注解或配置不确定时，先查仓库已有实现，不凭记忆补注解。
- 修改公共 API、DAO 方法、Service 签名或 XML `id` 前，先查影响面。
- 如果模块边界、通用常量归属、事务拆分方式存在歧义，及时向用户确认，不要擅自拍板。

按需读取：

- REST 层：见 [references/rest.md](references/rest.md)
- Entity / DO / DTO / QO / VO：见 [references/entity.md](references/entity.md)
- DAO 接口与 MyBatis XML：见 [references/dao-xml.md](references/dao-xml.md)
- Service 接口与实现：见 [references/service.md](references/service.md)
- common / peach-common 归属：见 [references/common.md](references/common.md)

与其他 skill 的关系：

- 涉及 README 时，同时使用 `$using-peach-readme-writer`。
- 涉及 RocketMQ、Storage、ThreadPool 时，在本 skill 之上叠加对应模块 skill。

补充约束：

- 新建或大改 Java 类时，类注释必须补齐 `@Author`、`@Version`、`@CreateTime`，并且与仓库现有时间格式保持一致。
- `rest`、`service`、`dao`、`entity` 这些层的注释不能只写功能名，必须说明业务边界、数据归属和模块职责。
- Service 接口的每个公开方法必须补齐 Javadoc；实现类若只继承接口语义，可以不重复写同一套说明。
- 如果新增代码里出现乱码、半截注释、明显口语化占位注释，本次修改要一起清理掉，不允许留下“以后再补”。
