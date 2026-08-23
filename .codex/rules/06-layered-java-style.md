# Layered Java Style

本规则区分新代码目标和存量兼容；安全与正确性要求见 `08-security-and-quality-gates.md`。

## Models

- `REQUIRED`：DO 对应持久化结构并继承 `PeachDO`；QO 只表达查询；DTO 只表达命令入参；VO 只表达允许返回的数据。
- `PREFERRED`：分页 QO 继承 `PeachEntity`；新增/更新复用 DTO 并使用 JSR-303 分组。
- `LEGACY_COMPATIBLE`：无敏感字段的简单 VO 可在维护存量模块时继承 DO。
- `FORBIDDEN`：含密码、token、secret、身份证等敏感字段的 DO 直接作为 VO 或被响应序列化；让前端 DTO 控制审计字段、逻辑删除或权限字段。

REST 入参必须触发 JSR-303 校验；Service 继续承担业务语义、权限和状态校验。校验分组沿用当前模块已有 `PeachGroup` 派生类，不为形式重复创建空分组类。

## DAO And XML

- DAO 继承 `PeachDao<T, E>`，自定义多参数方法显式使用 `@Param`。
- DAO 方法、XML `namespace/id`、参数类型、结果类型和字段片段必须同步。
- 修改公共 DAO 签名或 XML `id` 前评估调用方；修改后运行受影响模块测试或启动装配检查。
- 租户、组织、逻辑删除和状态条件属于数据完整性边界，不得为复用 SQL 随意省略。

## REST And Service

- REST 只负责绑定、校验、调用 Service 和包装响应；事务、DAO、线程控制和领域编排不得放入 Controller。
- `@Slf4j`、`@Indexed`、`@Validated` 按实际用途添加；未使用日志时不机械添加 `@Slf4j`。
- 写操作审计只记录非敏感业务标识和结果，禁止记录完整 DTO。
- Service 对外写方法按原子性需要使用 `@Transactional(rollbackFor = Exception.class)`；禁止依赖 private 方法或同类自调用触发代理。
- Spring Bean 依赖使用构造器注入 + `private final` 字段；单构造器使用 `@RequiredArgsConstructor`。
- 简单对象转换可沿用 `BeanUtils.copyProperties`，但主键、审计、权限、逻辑删除和敏感字段必须显式处理。

## API Compatibility

- 存量接口继续使用当前 `Response` 包装和模块路由，除非用户明确授权公共 API 演进。
- 非泛型 `Response`、动作式 CRUD 路由和 VO 继承 DO 视为存量兼容，不作为新公共 API 的默认目标。
- 新 API 设计有歧义时先确认，不以“仓库中数量最多”替代架构判断。
