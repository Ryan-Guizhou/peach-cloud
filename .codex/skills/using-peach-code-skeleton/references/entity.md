# Entity Layer

参考基线：

- `peach-auth/peach-auth-entity/src/main/java/com/peach/auth/entity/UserDO.java`
- `peach-auth/peach-auth-entity/src/main/java/com/peach/auth/vo/UserVO.java`
- `peach-auth/peach-auth-entity/src/main/java/com/peach/auth/qo/UserQO.java`
- `peach-auth/peach-auth-entity/src/main/java/com/peach/auth/dto/UserDTO.java`
- `peach-auth/peach-auth-entity/src/main/java/com/peach/auth/LoginInfo.java`
- 仓库 `Layered Model Rules`

当前仓库里的模型有稳定分工：

- `DO`：数据库持久化模型，带 `javax.persistence` 映射，通常继承 `PeachDO`。
- `VO`：返回视图，常直接继承对应 `DO`，必要时加序列化/Jackson 注解。
- `QO`：查询条件，常继承 `PeachEntity` 承载分页参数。
- `DTO`：新增/更新入参，常带 JSR-303 分组校验。
- 少量聚合模型：如 `LoginInfo`，不一定对应表结构，但会带 `@Schema` 和 Lombok。

通用规则：

- 类注释统一使用 Javadoc，保留 `@Author Mr Shu`、`@Version 1.0.0`、`@CreateTime yyyy/M/d HH:mm`。
- 字段说明优先写在 `@Schema(description = "...")`，不要为每个字段再补一层 Javadoc。
- 默认优先用 Lombok `@Data`；仅当当前模块已有不同风格时再跟随。
- 中文注释和 `@Schema` 如果在终端里显示乱码，先判断控制台编码，不要把乱码写回源文件。

DO 规则：

- 需要持久化映射的 DO，按现有风格补 `@Entity`、`@Table`、`@Id`、`@Column`。
- 公共审计字段统一继承 `PeachDO`，不要重复声明 `createdTime`、`creatorId`、`modifyTime`、`modifierId`。
- 大多数 DO 会实现 `Serializable` 并声明 `serialVersionUID`，新建时优先保持一致。
- 表字段、Java 字段、DAO XML 中的 alias/value/cond 必须同步。

VO 规则：

- 默认直接继承对应 DO。
- 当前仓库常见 `@JsonInclude(JsonInclude.Include.NON_NULL)`，尤其是对外返回模型；如果相邻 VO 已使用，保持一致。
- 只有展示层真的需要新增字段时，才在 VO 自己补字段，不重复定义父类已有属性。

QO 规则：

- 查询条件模型优先继承 `PeachEntity`。
- 常实现 `Serializable` 并声明 `serialVersionUID`。
- 分页字段通常不在 QO 内重复声明，直接复用父类。
- 只放查询条件，不混入保存/更新语义。

DTO 规则：

- DTO 只承载新增/更新入参，不承担查询职责。
- 按场景使用 JSR-303 分组校验，例如 `insertGroup`、`updateGroup`。
- 当前仓库里 DTO 类注释有时带 `@Description`，这是存量风格；新代码可以跟随当前模块，但不作为全仓强制。
- DTO 一般不加 `javax.persistence` 注解。

聚合模型规则：

- 像 `LoginInfo` 这种非持久化聚合对象，不加 `@Entity/@Table/@Column`。
- 仍建议补 `@Schema`、Lombok 和必要的集合字段说明。

案例 1：DO + VO

```java
/**
 * 用户实体。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime yyyy/M/d HH:mm
 */
@Data
@Entity
@Table(name = "PEACH_USER")
@Schema(description = "用户实体")
public class UserDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "USER_ID")
    @Schema(description = "用户ID")
    private String userId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;
}

@Data
@Schema(description = "用户返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO extends UserDO implements Serializable {
    private static final long serialVersionUID = 1L;
}
```

案例 2：QO + DTO

```java
@Data
@Schema(description = "用户查询参数")
public class UserQO extends PeachEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;
}

@Data
@Schema(description = "用户DTO")
public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @NotBlank(message = "用户ID不能为空", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String userId;
}
```

提交前检查：

- 当前模型到底是 `DO/VO/QO/DTO` 还是聚合对象，职责是否混淆。
- `DO` 是否错误地漏了 `@Column`/`@Id`，或把 `javax.persistence` 注解加到了 DTO/QO/VO。
- `VO` 是否真的需要新增字段，还是应直接继承 DO。
- `QO` 是否正确复用 `PeachEntity` 分页能力。
- `DTO` 的分组校验是否和 controller/service 的调用场景一致。
