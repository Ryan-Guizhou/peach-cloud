# DAO And XML

参考基线：

- `peach-common/src/main/java/com/peach/common/PeachDao.java`
- `peach-auth/peach-auth-service/src/main/java/com/peach/auth/dao/UserDao.java`
- `peach-auth/peach-auth-service/src/main/resources/com/peach/auth/dao/UserDao.xml`
- `peach-setting/peach-setting-service/src/main/resources/com/peach/setting/dao/DictTypeDao.xml`

当前仓库里的 DAO/XML 风格有三个稳定特征：

- DAO 接口统一继承 `PeachDao<T, E>`，基础 CRUD 由 XML 全量实现。
- XML 先放通用片段，再放基础 CRUD，最后放业务查询，例如 `selectByQO`、`login`、`selectByIdsAndOrgId`。
- 不同业务模块都尽量保持相同片段名和排版顺序，即使个别查询条件保留注释块或模块特有条件。

DAO 接口规则：

- DAO 接口统一继承 `PeachDao<T, E>`。
- `PeachDao` 定义的方法必须全部在 XML 中实现：
  - `insert`
  - `batchInsert`
  - `update`
  - `updateById`
  - `del`
  - `delById`
  - `delByIds`
  - `count`
  - `selectById`
  - `selectByIds`
  - `select`
- 自定义方法可以新增，但方法名、参数和返回值必须明确。
- 多参数方法统一显式标注 `@Param`。
- 接口类上沿用 `@Indexed`、`@MybatisDao`。

XML 结构规则：

- `namespace` 必须与 DAO 全限定名一致。
- 优先保持以下顺序：
  - `allColumn`
  - `allColumnAlias`
  - `allColumnValue`
  - `itemAllColumnValue`
  - `allColumnSet`
  - `updateSelectiveColumn`
  - `updateSelectiveValue`
  - `allColumnCond`
  - 基础 `insert/update/delete/select/count`
  - 业务自定义查询
- `parameterType`、`resultType`、`jdbcType` 显式声明，不要省略。
- 改 DAO 方法名时同步改 XML `id`；不要让 Spring/MyBatis 在运行期才暴露失配。

字段同步规则：

- 新增字段后，至少同步检查这些片段是否都补齐：
  - `allColumn`
  - `allColumnAlias`
  - `allColumnValue`
  - `itemAllColumnValue`
  - `allColumnSet`
  - `updateSelectiveColumn`
  - `updateSelectiveValue`
  - `allColumnCond`
- 如果模块存在组织、租户、逻辑删除、状态字段等业务约束，例如 `orgId`、`isDelete`、`status`，要同步检查它们是否应该进入 `where` 条件、`updateById` 条件或业务查询条件。

业务查询规则：

- 基础 CRUD 之外，仓库普遍会定义 `selectByQO`。
- `selectByQO` 的 `resultType` 通常是对应 `VO`，不是 `DO`。
- 允许保留与当前模块实现直接相关的注释或注释掉的条件块，例如 `DictTypeDao.xml` 里保留的 `<where>` 注释；不要为“代码整洁”擅自删掉存量设计痕迹，除非你确认它是无效噪音。

格式校验要求：

- 修改 XML 后至少人工检查片段顺序、缩进、空行、标签闭合和 `id`/DAO 方法对应关系。
- 如果环境可用，优先跑受影响模块编译，利用 MyBatis/Spring 装配暴露错误。
- 不引入项目里不存在的新 XML 书写风格，例如突然改成 `resultMap` 主导、或把已有片段体系整套替换掉。

案例 1：最小 PeachDao 骨架

```xml
<mapper namespace="com.peach.xxx.dao.XxxDao">
    <sql id="allColumn">...</sql>
    <sql id="allColumnAlias">...</sql>
    <sql id="allColumnValue">...</sql>
    <sql id="itemAllColumnValue">...</sql>
    <sql id="allColumnSet">...</sql>
    <sql id="updateSelectiveColumn">...</sql>
    <sql id="updateSelectiveValue">...</sql>
    <sql id="allColumnCond">...</sql>

    <insert id="insert" parameterType="com.peach.xxx.entity.XxxDO">...</insert>
    <insert id="batchInsert" parameterType="com.peach.xxx.entity.XxxDO">...</insert>
    <update id="update" parameterType="com.peach.xxx.entity.XxxDO">...</update>
    <delete id="delById" parameterType="string">...</delete>
    <delete id="delByIds" parameterType="java.util.List">...</delete>
    <delete id="del" parameterType="com.peach.xxx.entity.XxxDO">...</delete>
    <update id="updateById" parameterType="com.peach.xxx.entity.XxxDO">...</update>
    <select id="selectById" parameterType="string" resultType="com.peach.xxx.entity.XxxDO">...</select>
    <select id="selectByIds" parameterType="java.util.List" resultType="com.peach.xxx.entity.XxxDO">...</select>
    <select id="select" parameterType="com.peach.xxx.entity.XxxDO" resultType="com.peach.xxx.entity.XxxDO">...</select>
    <select id="count" parameterType="com.peach.xxx.entity.XxxDO" resultType="java.lang.Integer">...</select>
</mapper>
```

案例 2：业务查询骨架

```xml
<select id="selectByQO" parameterType="com.peach.xxx.qo.XxxQO" resultType="com.peach.xxx.vo.XxxVO">
    SELECT
        <include refid="allColumnAlias" />
    FROM PEACH_XXX
    <where>
        <if test="id != null and id != ''">
            AND ID = #{id,jdbcType=VARCHAR}
        </if>
        <if test="status != null">
            AND STATUS = #{status,jdbcType=INTEGER}
        </if>
    </where>
    ORDER BY MODIFY_TIME DESC
</select>
```

提交前检查：

- DAO 是否仍完整承接 `PeachDao` 约定的方法。
- 自定义查询是否用了明确的 `parameterType/resultType`。
- 字段新增后，八个通用片段是否同步更新。
- 租户/组织/逻辑删除字段是否遗漏在关键查询或更新条件里。
- `selectByQO`、`selectByIdAndOrgId` 这类业务查询是否保持 VO 返回语义。
