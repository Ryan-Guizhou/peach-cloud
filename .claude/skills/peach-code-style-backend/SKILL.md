---
name: peach-code-style-backend
description: |
  Peach 后端代码风格规范。编写 Java 后端代码时必须遵循此 Skill。
  确保生成代码与 Peach 框架现有风格完全一致。
metadata:
  short-description: Peach 后端 Java 代码风格约束
  author: Mr Shu
---

# Peach 后端代码风格规范

## 技术基线
- **JDK**: 1.8（禁止 `var`/`List.of()`/`record` 等 Java 9+ 语法）
- **Spring Boot**: 2.7.x | **MyBatis**: 2.3.1 原生 XML | **knife4j**: 4.4.0

## 模块与包路径
```
peach-xxx-entity/ → DO/DTO/QO/VO | peach-xxx-service/ → Service/DAO
peach-xxx-rest/   → Controller   | peach-xxx-common/   → 常量/枚举/工具
```
根包：`com.peach.<module>`

## 命名规范
| 类型 | 规则 | 示例 |
|------|------|------|
| Controller | `XxxController` | `FileController` |
| Service 接口 | `IXxxService` | `IFileService` |
| Service 实现 | `XxxServiceImpl` | `FileServiceImpl` |
| DAO | `XxxDao` + `@MybatisDao` | `RoleDao` |
| DO | `XxxDO extends PeachDO` | `SysConfigDO` |
| DTO/QO/VO | 独立类 | `SysConfigDTO` |

> **重要**：DO 类继承 `PeachDO` 包含 8 个审计字段（createTime/createUserId/createUserCode/createUserName/modifyTime/modifyUserId/updateUserCode/updateUserName），**不得重复定义**

## Controller 规范
- 注解：`@Tag` + `@Slf4j` + `@Indexed` + `@RestController` + `@RequestMapping`
- 方法必须 `@Operation(summary)`
- 参数必须 `@Validated(Group.class)`（仅 QO/DTO 有校验规则时）
- 方法命名：`query`/`save`/`modify`/`delete` 开头
- 返回 `Response`（不带泛型）

**@UserOperLog 规范**：
- `save`/`modify`/`delete` 方法必须添加
- `moduleCode`: **必须使用枚举** `UserLogEnum.Module.XXX`
- 示例：`moduleCode = UserLogEnum.Module.SYSCONFIG`

## Service 规范
- 接口返回 `Response`，实现类 `@Slf4j` + `@Indexed` + `@Service`
- 注入：业务 Bean → `@Resource`；Spring Bean → `@Autowired`

## DAO 规范
- `@Indexed` + `@MybatisDao`，继承 `PeachDao<DO, VO>`

## MyBatis XML 规范
- 必须包含 SQL 片段：`allColumn`/`allColumnAlias`/`allColumnValue`/`allColumnCond`/`allColumnSet`
- `namespace` = DAO 全限定名，字段指定 `jdbcType`
- **禁止** `SELECT *`

## Entity 规范
- DO 继承 `PeachDO`，实现 `Serializable`，**不得重写父类字段方法**
- VO 继承 DO，加 `@JsonInclude(NON_NULL)`
- DTO 使用分组校验（insert 时 ID `@Null`，update 时 `@NotNull`）
- QO 继承 `PeachEntity`（分页）

## 类注释
```java
/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/21 10:00
 * @Description {描述}
 */
```

## 禁止事项
1. Java 9+ 语法 | 2. `System.out.println` | 3. 硬编码魔法值
4. Controller 写业务逻辑 | 5. 吞异常 | 6. MyBatis XML `SELECT *`
7. 跨模块复制包名 | 8. 无关重构

## 强制校验
```bash
# Windows
powershell -File .claude/peach-code-style-backend/scripts/check-style.ps1 -Target <目录>
```
**11 项检查**：Java 9+语法、System.out、Controller注解、Service注解、DAO注解、命名规范、XML规范、Serializable、方法命名与@UserOperLog、参数校验、主键时间字段

**规则**：`[ERROR]` 必须修复 | `[WARN]` 建议修复 | 校验未通过禁止提交