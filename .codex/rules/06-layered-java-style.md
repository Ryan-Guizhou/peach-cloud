# Layered Java Style

本规则用于约束 `peach-cloud` 从 DO 到 REST 的基础代码风格和分层职责。

## Model Layer

- DO 完全适配数据库表结构和字段语义
- DO 统一继承 `PeachDO`，复用公共审计字段
- VO 默认继承对应 DO；仅在展示层需要时新增额外属性
- QO 只用于查询条件封装
- QO 有分页需求时继承 `PeachEntity`
- DTO 只用于新增和修改，不承担查询职责

## Validation

- DTO、QO、ID 等前端传入参数必须使用 JSR-303 校验
- 需要区分场景时必须使用分组校验，按 `PeachGroup.insertGroup`、`updateGroup`、`deleteGroup`、`queryGroup` 组织
- REST 入参不能跳过校验直接透传 service
- 同一实体的新增、更新场景优先复用一个 DTO，通过校验分组区分语义；不要在没有明确需求时主动拆成两个 DTO
- 如果任务同时涉及 controller、service、DTO、group 和日志注解，必须把这一组改动视为一个整体完成，不能只补其中一层

## DAO Contract

- 所有 DAO 接口统一继承 `PeachDao<T, E>`
- DAO 必须实现 `PeachDao` 约定的基础方法
- 如有额外查询或更新需求，可新增方法，但必须声明明确的入参和出参类型
- 不使用模糊返回值或无类型容器让调用方猜测结构

## MyBatis XML Style

- MyBatis XML 编写风格参考 `peach-auth/peach-auth-service/src/main/resources/com/peach/auth/dao/UserDao.xml`
- 保持现有缩进方式、SQL 片段拆分方式和标签布局一致
- `jdbcType` 必须显式声明，并与同类字段现有写法保持一致
- `parameterType`、`resultType`、`resultMap` 不能省略到影响可读性或一致性的程度
- 新增 SQL 方法时，参数名、别名、集合遍历和条件片段写法保持与现有 DAO 一致

## REST And Service Boundary

- REST 层只负责接收参数、触发校验、调用 service、返回 `Response`
- 复杂业务逻辑、事务、领域编排放在 service 层
- 不在 REST 层直接拼接复杂 SQL、处理核心领域逻辑或散落校验规则
