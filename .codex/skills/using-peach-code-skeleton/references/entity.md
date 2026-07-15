# Entity Layer

模型按数据职责设计，不以历史继承关系替代安全边界。

## 导航结构

```text
peach-auth/peach-auth-entity/src/main/java/com/peach/auth/
├── entity/
│   └── UserDO.java                       # 数据库存储模型
├── dto/
│   └── UserDTO.java                      # 命令入参
├── qo/
│   └── UserQO.java                       # 查询条件
├── vo/
│   └── UserVO.java                       # 对外返回视图
└── group/
    └── UserGroup.java                    # JSR-303 场景分组
```

定位模型时同时检查 Controller、Service、DAO/XML 和序列化链路，不能只看类名。

## REQUIRED

- DO 映射持久化结构；DTO 表达新增/更新或业务命令；QO 只表达查询；VO 只包含允许返回的数据。
- 含 password、token、secret、privateKey、身份证等敏感字段的 DO 禁止直接作为 VO 或被响应序列化。
- 前端不得控制创建人、修改人、租户、逻辑删除、权限和安全状态等服务端字段。
- DTO/QO 的校验分组必须由 REST 真实触发，Service 继续校验业务语义。
- DO 字段、数据库列和 DAO XML 同步；时间、金额、状态等字段使用能表达真实语义的类型。
- 含敏感字段的 Lombok 模型必须排除 `toString()`/日志暴露风险。

## PREFERRED

- 高风险业务使用独立 VO 显式声明返回字段，不依赖 `@JsonIgnore` 或调用方手工置空。
- 分页 QO 复用 `PeachEntity`；新增/更新优先使用一个 DTO + 分组校验，除非语义差异足够大。
- 字段文档使用 `@Schema`，复杂边界在类或接口契约说明，不堆重复 Javadoc。
- 状态使用 enum 或受约束值对象，避免任意字符串在各层传播。

## LEGACY_COMPATIBLE

- 无敏感字段的简单 `VO extends DO` 可在局部维护时保留，但不作为新模型默认。
- `javax.persistence` 注解、`PeachDO`、`PeachEntity` 和现有 group 命名按当前框架兼容。
- 历史拼写错误字段只能在兼容数据库/API 时保留；新字段不得继续复制错误命名。

## FORBIDDEN

- DTO/QO 添加持久化注解或承担数据库实体职责。
- VO 因继承 DO 暴露密码、审计内部字段、逻辑删除或权限字段。
- 使用无类型 Map 代替稳定业务模型。
- 为减少类数量混用查询、命令、持久化和响应职责。

## 验证

- 逐字段检查输入来源、持久化去向、日志和响应输出。
- 检查校验分组、序列化注解、DAO/XML 和 API 文档。
- 运行受影响模块测试、UTF-8 检查与 `git diff --check`。
