# DAO And MyBatis XML

当前源码用于确认契约和兼容性，不自动等于新代码目标。DAO/XML 的首要要求是参数、SQL、租户边界和返回模型一致。

## 导航结构

```text
peach-cloud/
├── peach-common/src/main/java/com/peach/common/
│   ├── PeachDao.java                     # DAO 基础契约
│   └── annoation/MybatisDao.java         # Mapper 标识
└── peach-auth/peach-auth-service/src/main/
    ├── java/com/peach/auth/dao/
    │   └── UserDao.java                  # DAO 接口参考
    └── resources/com/peach/auth/dao/
        └── UserDao.xml                   # XML 契约参考
```

处理其他模块时，按相同的 `src/main/java/.../dao` 与 `src/main/resources/.../dao` 对照定位，不能只修改一侧。

## REQUIRED

- DAO 与 XML `namespace`、方法名/`id`、参数、返回类型必须一一对应。
- 继承 `PeachDao<T, E>` 时，当前契约要求的方法必须有明确实现；修改基础契约前先做全仓影响分析。
- 多参数方法显式使用 `@Param`；SQL 中只引用已命名参数。
- `jdbcType`、`parameterType`、`resultType/resultMap` 按项目契约显式声明。
- 新增字段同步检查列清单、别名、插入、更新、条件和结果映射。
- 租户、组织、逻辑删除、权限和状态条件属于数据安全边界，查询与更新不得遗漏。
- 动态排序、表名和列名不得直接拼接未校验的外部输入。

## PREFERRED

- XML 按“公共片段 → 基础 CRUD → 业务查询”组织，并让片段名表达用途。
- 查询只选择调用方需要的字段；敏感字段不得因 `allColumn` 被带入响应模型。
- 批量操作明确空集合、最大批次、事务和部分失败语义。
- 复杂查询优先使用明确的 QO 和 VO，不使用无类型 `Map` 传递业务结构。

## LEGACY_COMPATIBLE

- `allColumn*` 八片段和 XML 全量 CRUD 是当前模块兼容模式；新增模块可复用，但不得因此复制无关字段或不安全条件。
- `selectByQO`、历史方法命名和 XML 排版可在局部维护时保持，不作为正确性依据。
- `@Indexed` 仅在确有索引需求时保留，不是 DAO 必选注解。

## FORBIDDEN

- 仅修改 DAO 或仅修改 XML。
- 通过 `${}` 拼接未经白名单校验的请求参数。
- 为保持历史片段而返回密码、token、secret 等敏感列。
- 删除租户、逻辑删除或权限条件以“修复查不到数据”。
- 依赖运行期启动才发现 mapper 失配。

## 验证

1. 搜索 DAO 方法全部调用方和 XML `id`。
2. 对照字段片段与 DO/QO/VO。
3. 运行 mapper smoke test 或受影响模块测试。
4. 运行 `node scripts/check-utf8.mjs` 与 `git diff --check`。
