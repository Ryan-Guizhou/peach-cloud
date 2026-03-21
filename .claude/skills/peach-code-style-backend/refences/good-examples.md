# Peach 后端代码风格规范 - 正确示例

本文档展示符合 Peach 后端代码规范的正确代码示例。

---

## 1. DO 基类 (BaseDO)

```java
package com.peach.common;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description DO基类 - 包含所有实体类的公共审计字段
 *               包含：创建时间、创建者信息、更新时间、更新者信息
 */
@Data
public class PeachDO implements Serializable {

    private static final long serialVersionUID = 1967646335443236468L;

    @Column(name = "CREATE_TIME")
    private String createTime;     // 记录创建时间，格式：yyyy-MM-dd HH:mm:ss

    @Column(name = "CREATE_USER_ID")
    private String createUserId;   // 创建人ID，用于审计追踪

    @Column(name = "CREATE_USER_CODE")
    private String createUserCode; // 创建人账号

    @Column(name = "CREATE_USER_NAME")
    private String createUserName; // 创建人姓名

    @Column(name = "MODIFY_TIME")
    private String modifyTime;     // 记录最后修改时间

    @Column(name = "MODIFY_USER_ID")
    private String modifyUserId;  // 修改人ID

    @Column(name = "UPDATE_USER_CODE")
    private String updateUserCode; // 修改人账号

    @Column(name = "UPDATE_USER_NAME")
    private String updateUserName; // 修改人姓名
}
```

---

## 2. 实体类 (DO) - 继承 PeachDO

```java
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 文件索引实体 - 继承PeachDO获取审计字段
 *               主键为String类型(UUID)，时间字段为String类型
 */
@Data
@Entity
@Table(name = "FILE_INDEX")          // 实体映射表名
@Schema(description = "FILE_INDEX实体") // Swagger文档描述
public class FileIndexDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1967646335443236467L;

    @Id
    @Column(name = "ID", length = 36)
    private String id;              // 主键ID，使用String类型（UUID），禁止自增

    @Column(name = "FILE_NAME", length = 255)
    private String fileName;        // 文件名称

    @Column(name = "FILE_PATH", length = 500)
    private String filePath;        // 文件存储路径

    @Column(name = "FILE_SIZE")
    private Long fileSize;          // 文件大小（字节）

    // 时间字段已从PeachDO继承：createTime, modifyTime（String类型，VARCHAR）
}
```

---

## 2.1 DO 主键规范（UUID + 非自增）

```java
/**
 * 正确：主键为 String 类型（UUID）
 */
@Id
@Column(name = "主键", length = 36)
private String id;

/**
 * 错误：禁止使用以下方式
 * 1. Long 类型主键 + 自增
 * 2. Integer 类型主键 + 自增
 */
// 错误示例
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)  // ❌ 禁止
private Long id;
```

---

## 2.2 时间字段规范（String + VARCHAR）

```java
/**
 * PeachDO 中的时间字段定义
 * Java: String 类型
 * 数据库: VARCHAR 类型
 * 格式: yyyy-MM-dd HH:mm:ss
 */
@Column(name = "创建时间")
private String createTime;     // 记录创建时间

@Column(name = "更新时间")
private String modifyTime;     // 记录最后修改时间

/**
 * 错误：禁止使用以下方式
 */
// 错误示例
private Date createTime;        // ❌ 禁止
private LocalDateTime createTime;  // ❌ 禁止
private Timestamp createTime;   // ❌ 禁止
```

---

## 3. 视图对象 (VO) - 继承 DO

```java
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 文件索引视图对象 - 用于API返回数据
 *               继承DO获取所有字段，可扩展额外业务字段
 */
@Data
@Schema(description = "文件索引视图对象")
// JsonInclude.NON_NULL：仅序列化非空字段，减少响应数据量
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileIndexVO extends FileIndexDO implements Serializable {

    private static final long serialVersionUID = 1967646335443236467L;

    @Schema(description = "附件列表")   // Swagger文档注释
    private List<String> attachList;     // 业务扩展字段（VO特有）

}
```

---

## 4. 分组校验 (Group)

### 4.1 基础分组定义

```java
package com.peach.common;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 * @Description 基础分组规则 - 定义增删改查四种操作分组
 *               用于 @Validated 注解指定校验规则分组
 */
public class PeachGroup {

    /** 新增操作分组 */
    public interface insertGroup{
    }

    /** 更新操作分组 */
    public interface updateGroup{
    }

    /** 删除操作分组 */
    public interface deleteGroup{

    }

    /** 查询操作分组 */
    public interface queryGroup{
    }
}
```

### 4.2 业务分组继承

```java
import com.peach.common.PeachGroup;
/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 文件索引分组规则 - 继承PeachGroup并扩展业务分组
 *               在基础分组上增加业务特有校验规则
 */
public class FileIndexGroup extends PeachGroup{

    /** 新增文件分组 - 继承基础新增组，可添加业务特有校验 */
    public interface addFile{
    }
}
```

---

## 5. 分页查询基类 (PeachEntity)

```java
package com.peach.common;

import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:42
 */
@Data
public class PeachEntity implements Serializable {

    private Integer pageNum = 1;

    private Integer pageSize = 20;

    public <T extends PeachEntity> T clone(Map source) {
        try {
            BeanUtils.copyProperties(this, source);
            return (T) this;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends PeachEntity> T create(Class<T> c) throws RuntimeException {
        try {
            T t = c.newInstance();
            return t;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

```

---

## 6. 查询参数类 (QO) - 继承 PeachEntity

```java
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import com.peach.common.PeachEntity;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 文件索引查询参数 - 用于分页查询和列表查询
 *               继承PeachEntity获得分页功能，添加业务查询条件
 */
@Data
@Schema(description = "文件索引查询参数")  // Swagger文档描述
public class FileIndexQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1967646335443236467L;

    @Schema(description = "主键ID列表", example = "['id1', 'id2']")
    @NotNull(message = "更新时ID不能为空", groups = FileIndexGroup.Query.class)
    private List<String> idList;  // 用于批量查询/删除

}
```

---

## 7. 数据传输对象 (DTO) - 分组校验示例

```java
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Size;

import javax.validation.constraints.*;
import java.io.Serializable;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 文件索引保存对象 - 用于新增和更新
 *               使用分组校验：新增时ID为空，更新时ID必填
 */
@Data
@Schema(description = "文件索引保存对象")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileIndexDTO implements Serializable {

    private static final long serialVersionUID = 1967646335443236467L;

    /** ==================== 分组校验规则说明 ====================
     * insertGroup: 新增时校验 - ID必须为空、必填字段不能为空
     * updateGroup: 更新时校验 - ID不能为空、必填字段不能为空
     * ======================================================= */

    @Schema(description = "主键ID", requiredMode = NOT_REQUIRED)
    @NotNull(message = "更新时ID不能为空", groups = FileIndexGroup.updateGroup.class)  // 更新组必填
    @Null(message = "新增时不可指定ID", groups = FileIndexGroup.insertGroup.class)  // 新增组必须为空
    private String id;

    @Schema(description = "文件名称", requiredMode = REQUIRED, example = "test.pdf")
    @NotBlank(message = "文件名不能为空", groups = {FileIndexGroup.insertGroup.class, FileIndexGroup.updateGroup.class})
    @Length(max = 255, message = "文件名最长255字符")
    private String fileName;

    @Schema(description = "文件绝对路径", requiredMode = NOT_REQUIRED, example = "/data/upload/")
    @NotBlank(message = "文件路径不能为空", groups = {FileIndexGroup.insertGroup.class, FileIndexGroup.updateGroup.class})
    @Size(max = 500, message = "文件路径最长500字符")
    private String filePath;

    @Schema(description = "备注信息")
    private String remark;
}
```

---

## 8. Controller 示例

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 文件管理控制器 - 负责API请求入口
 *               职责：参数校验、调用Service、返回Response
 *
 * ==================== 必要注解说明 ====================
 * @Tag:              Swagger文档分组
 * @Slf4j:            日志输出（禁止System.out.println）
 * @Indexed:           提升Spring索引性能
 * @RestController:    RESTful接口
 * @RequestMapping:   路由前缀
 * ====================================================
 */
@Tag(name = "文件管理", description = "文件索引的增删改查")
@Slf4j // 日志统一输出
@Indexed // 用于优化性能
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Operation(summary = "分页查询文件")
    @PostMapping("/queryPage")
    public Result<PageVO<FileVO>> queryPage(@Validated(FileIndexGroup.query.class) @RequestBody FileQueryQO queryQO) {
        return Result.success(fileService.queryPage(queryQO));
    }

    @Operation(summary = "保存文件索引")
    @PostMapping("/saveFile")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'新增索引信息:['+#p0+']'")
    public Result<Boolean> saveFile(@Validated(FileIndexGroup.addGroup.class) @RequestBody FileSaveDTO saveDTO) {
        return Result.success(fileService.saveFile(saveDTO));
    }

    @Operation(summary = "根据ID删除索引")
    @DeleteMapping("deleteById/{id}")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除索引 索引ID:['+#p0+']'")
    public Result<Boolean> deleteById(@PathVariable String id) {
        return Result.success(fileService.deleteById(id));
    }

    @Operation(summary = "根据ID更新数据")
    @PostMapping("/modifyById")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'更新索引 索引ID:['+#p0.id+']'")
    public Result<Boolean> modifyById(@Validated(FileIndexGroup.updateGroup.class) @RequestBody FileSaveDTO saveDTO) {
        return Result.success(fileService.updateById(saveDTO));
    }
}
```

---

## 9. Service 接口

```java
import com.peach.common.response.Response;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 文件服务接口 - 定义业务操作规范
 *               命名规范：I + 业务名 + Service
 */
public interface IFileService {

    /**
     * 分页查询
     * @param queryQO 查询参数
     * @return 分页结果
     */
    Response queryPage(FileIndexQO queryQO);

    /**
     * 保存文件
     * @param dto 保存数据
     * @return 操作结果
     */
    Response saveFile(FileIndexDTO dto);

    /**
     * 更新文件
     * @param dto 更新数据
     * @return 操作结果
     */
    Response updateById(FileIndexDTO dto);

    /**
     * 删除文件
     * @param id 文件ID
     * @return 操作结果
     */
    Response deleteById(String id);
}
```

---

## 10. ServiceImpl 实现

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 文件服务实现 - 业务逻辑处理层
 *               职责：调用DAO、事务管理、业务逻辑处理
 *
 * ==================== 必要注解说明 ====================
 * @Slf4j:    日志输出
 * @Indexed:  提升索引性能
 * @Service: Spring容器管理
 * ====================================================
 */
@Slf4j
@Indexed
@Service
public class FileServiceImpl implements IFileService {

    // ==================== 依赖注入规范 ====================
    // @Resource:  注入自己编写的业务DAO/Service
    // @Autowired: 注入Spring框架提供的Bean
    // ====================================================
    @Resource
    private FileDao fileDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Response queryPage(FileIndexQO queryQO) {
        // 业务逻辑实现
        return null;
    }

    @Override
    public Response saveFile(FileIndexDTO dto) {
        // 业务逻辑实现
        return null;
    }

    @Override
    public Response updateById(FileIndexDTO dto) {
        // 业务逻辑实现
        return null;
    }

    @Override
    public Response deleteById(String id) {
        // 业务逻辑实现
        return null;
    }
}
```

---

## 11. DAO 接口

```java
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 16:21
 * @Description 文件索引数据访问接口
 *
 * ==================== 必要注解说明 ====================
 * @Indexed:    提升查询性能
 * @MybatisDao:  MyBatis扫描标记
 * 继承PeachDao: 获得基础CRUD方法（select/insert/update/delete等）
 * ========================================================
 *
 * @param <T> DO实体类型
 * @param <E> VO视图类型
 */
@Indexed
@MybatisDao
public interface FileIndexDao extends PeachDao<FileIndexDO, FileIndexVO> {

    /**
     * 自定义查询方法 - 根据QO条件查询
     * @param qO 查询参数
     * @return 查询结果列表
     */
    List<FileIndexVO> selectByQO(FileIndexQO qo);
}
```

---

## 12. MyBatis XML Mapper

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!-- namespace必须与DAO接口全限定名一致 -->
<mapper namespace="com.peach.auth.dao.FileIndexDao">

    <!-- ==================== SQL片段说明 ==================== -->
    <!-- allColumn:        所有数据库字段名 -->
    <!-- allColumnAlias:   字段别名（映射到Java属性） -->
    <!-- allColumnValue:   INSERT时字段值 -->
    <!-- itemAllColumnValue: 批量插入时字段值 -->
    <!-- allColumnSet:     UPDATE SET部分 -->
    <!-- allColumnCond:    WHERE条件（动态if判断） -->
    <!-- ==================================================== -->

    <!-- 1. 所有列名（INSERT/SELECT使用） -->
    <sql id="allColumn">
        ID,                    FILE_NAME,      FILE_PATH,      FILE_SIZE,
        CREATE_TIME,           CREATE_USER_ID, CREATE_USER_CODE, CREATE_USER_NAME,
        MODIFY_TIME,           MODIFY_USER_ID, MODIFY_USER_CODE, MODIFY_USER_NAME
    </sql>

    <!-- 2. 列别名（SELECT结果映射） - 必须指定jdbcType -->
    <sql id="allColumnAlias">
        ID as id,                    FILE_NAME as fileName,         FILE_PATH as filePath,     FILE_SIZE as fileSize,
        CREATE_TIME as createTime,   CREATE_USER_ID as createUserId, CREATE_USER_CODE as createUserCode, CREATE_USER_NAME as createUserName,
        MODIFY_TIME as modifyTime,    MODIFY_USER_ID as modifyUserId,  MODIFY_USER_CODE as updateUserCode, MODIFY_USER_NAME as updateUserName
    </sql>

    <!-- 3. INSERT字段值 - 必须指定jdbcType -->
    <sql id="allColumnValue">
        #{id,jdbcType=VARCHAR}, #{fileName,jdbcType=VARCHAR},   #{filePath,jdbcType=VARCHAR},   #{fileSize,jdbcType=BIGINT},
        #{createTime,jdbcType=VARCHAR}, #{createUserId,jdbcType=VARCHAR}, #{createUserCode,jdbcType=VARCHAR}, #{createUserName,jdbcType=VARCHAR},
        #{modifyTime,jdbcType=VARCHAR}, #{modifyUserId,jdbcType=VARCHAR}, #{updateUserCode,jdbcType=VARCHAR}, #{updateUserName,jdbcType=VARCHAR}
    </sql>

    <!-- 4. 批量插入字段值 -->
    <sql id="itemAllColumnValue">
        #{item.id,jdbcType=VARCHAR}, #{item.fileName,jdbcType=VARCHAR}, #{item.filePath,jdbcType=VARCHAR}, #{item.fileSize,jdbcType=BIGINT},
        #{item.createTime,jdbcType=VARCHAR}, #{item.createUserId,jdbcType=VARCHAR}, #{item.createUserCode,jdbcType=VARCHAR}, #{item.createUserName,jdbcType=VARCHAR},
        #{item.modifyTime,jdbcType=VARCHAR}, #{item.modifyUserId,jdbcType=VARCHAR}, #{item.updateUserCode,jdbcType=VARCHAR}, #{item.updateUserName,jdbcType=VARCHAR}
    </sql>

    <!-- 5. UPDATE SET部分（动态条件） -->
    <sql id="allColumnSet">
        <trim suffixOverrides=",">
            <if test="fileName != null and fileName != ''">
                FILE_NAME = #{fileName,jdbcType=VARCHAR},
            </if>
            <if test="filePath != null and filePath != ''">
                FILE_PATH = #{filePath,jdbcType=VARCHAR},
            </if>
            <if test="fileSize != null">
                FILE_SIZE = #{fileSize,jdbcType=BIGINT},
            </if>
            <!-- 审计字段自动处理 -->
            <if test="createTime != null and createTime != ''">
                CREATE_TIME = #{createTime,jdbcType=VARCHAR},
            </if>
            <if test="modifyTime != null and modifyTime != ''">
                MODIFY_TIME = #{modifyTime,jdbcType=VARCHAR},
            </if>
        </trim>
    </sql>

    <!-- 6. WHERE条件（动态if判断） -->
    <sql id="allColumnCond">
        <if test="id != null and id != ''">
            AND ID = #{id,jdbcType=VARCHAR}
        </if>
        <if test="fileName != null and fileName != ''">
            AND FILE_NAME = #{fileName,jdbcType=VARCHAR}
        </if>
        <if test="filePath != null and filePath != ''">
            AND FILE_PATH = #{filePath,jdbcType=VARCHAR}
        </if>
        <if test="fileSize != null">
            AND FILE_SIZE = #{fileSize,jdbcType=BIGINT}
        </if>
    </sql>

    <!-- ==================== 基础CRUD操作 ==================== -->

    <!-- 插入 -->
    <insert id="insert" parameterType="com.peach.auth.entity.FileIndexDO">
        INSERT INTO FILE_INDEX (<include refid="allColumn" />)
        VALUES (<include refid="allColumnValue" />)
    </insert>

    <!-- 批量插入 -->
    <insert id="batchInsert" parameterType="com.peach.auth.entity.FileIndexDO">
        INSERT INTO FILE_INDEX (<include refid="allColumn" />)
        VALUES
        <foreach collection="list" index="index" item="item" separator=",">
            (<include refid="itemAllColumnValue"/>)
        </foreach>
    </insert>

    <!-- 更新（条件更新） -->
    <update id="update" parameterType="com.peach.auth.entity.FileIndexDO">
        UPDATE FILE_INDEX SET <include refid="allColumnSet" />
        <where><include refid="allColumnCond" /></where>
    </update>

    <!-- 根据ID删除 -->
    <delete id="delById" parameterType="string">
        DELETE FROM FILE_INDEX WHERE ID = #{value,jdbcType=VARCHAR}
    </delete>

    <!-- 批量删除 -->
    <delete id="delByIds" parameterType="java.util.List">
        DELETE FROM FILE_INDEX WHERE ID IN
        <foreach collection="list" index="index" item="id" open="(" separator="," close=")">
            #{id,jdbcType=VARCHAR}
        </foreach>
    </delete>

    <!-- 根据ID更新 -->
    <update id="updateById" parameterType="com.peach.auth.entity.FileIndexDO">
        UPDATE FILE_INDEX SET <include refid="allColumnSet" />
        WHERE ID = #{id,jdbcType=VARCHAR}
    </update>

    <!-- 根据ID查询 -->
    <select id="selectById" parameterType="string" resultType="com.peach.auth.vo.FileIndexVO">
        SELECT <include refid="allColumnAlias" /> FROM FILE_INDEX
        WHERE ID = #{value,jdbcType=VARCHAR}
    </select>

    <!-- 批量查询 -->
    <select id="selectByIds" parameterType="java.util.List" resultType="com.peach.auth.vo.FileIndexVO">
        SELECT <include refid="allColumnAlias" /> FROM FILE_INDEX
        WHERE ID IN
        <foreach collection="list" index="index" item="id" open="(" separator="," close=")">
            #{id,jdbcType=VARCHAR}
        </foreach>
    </select>

    <!-- 条件查询 -->
    <select id="select" parameterType="com.peach.auth.entity.FileIndexDO" resultType="com.peach.auth.vo.FileIndexVO">
        SELECT <include refid="allColumnAlias" /> FROM FILE_INDEX
        <where><include refid="allColumnCond" /></where>
    </select>

    <!-- 统计数量 -->
    <select id="count" parameterType="com.peach.auth.entity.FileIndexDO" resultType="java.lang.Integer">
        SELECT COUNT(1) FROM FILE_INDEX
        <where><include refid="allColumnCond" /></where>
    </select>

    <!-- 自定义查询 -->
    <select id="selectByQO" parameterType="com.peach.auth.qo.FileIndexQO" resultType="com.peach.auth.vo.FileIndexVO">
        SELECT <include refid="allColumnAlias" /> FROM FILE_INDEX
        <where>
            <if test="idList != null and idList.size() > 0">
                AND ID IN
                <foreach collection="idList" item="id" open="(" separator="," close=")">
                    #{id}
                </foreach>
            </if>
        </where>
    </select>

</mapper>
```

---

## 12.1 MyBatis XML - databaseIdProvider 多数据库兼容

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.peach.auth.dao.FileIndexDao">

    <!-- 使用 databaseId 实现多数据库兼容 -->
    <!-- MySQL: 使用 NOW() 获取当前时间 -->
    <select id="selectByTimeRange" parameterType="com.peach.auth.qo.FileIndexQO"
            resultType="com.peach.auth.vo.FileIndexVO" databaseId="mysql">
        SELECT <include refid="allColumnAlias" /> FROM FILE_INDEX
        WHERE CREATE_TIME &gt;= #{startTime,jdbcType=VARCHAR}
        AND CREATE_TIME &lt;= #{endTime,jdbcType=VARCHAR}
    </select>

    <!-- Oracle: 使用 TO_DATE 进行时间比对 -->
    <select id="selectByTimeRange" parameterType="com.peach.auth.qo.FileIndexQO"
            resultType="com.peach.auth.vo.FileIndexVO" databaseId="oracle">
        SELECT <include refid="allColumnAlias" /> FROM FILE_INDEX
        WHERE CREATE_TIME &gt;= TO_DATE(#{startTime,jdbcType=VARCHAR}, 'YYYY-MM-DD HH24:MI:SS')
        AND CREATE_TIME &lt;= TO_DATE(#{endTime,jdbcType=VARCHAR}, 'YYYY-MM-DD HH24:MI:SS')
    </select>

    <!-- PostgreSQL: 使用 TO_TIMESTAMP 进行时间比对 -->
    <select id="selectByTimeRange" parameterType="com.peach.auth.qo.FileIndexQO"
            resultType="com.peach.auth.vo.FileIndexVO" databaseId="postgresql">
        SELECT <include refid="allColumnAlias" /> FROM FILE_INDEX
        WHERE CREATE_TIME &gt;= TO_TIMESTAMP(#{startTime,jdbcType=VARCHAR}, 'YYYY-MM-DD HH24:MI:SS')
        AND CREATE_TIME &lt;= TO_TIMESTAMP(#{endTime,jdbcType=VARCHAR}, 'YYYY-MM-DD HH24:MI:SS')
    </select>

    <!-- 通用SQL（不指定 databaseId） -->
    <select id="selectActiveFiles" resultType="com.peach.auth.vo.FileIndexVO">
        SELECT <include refid="allColumnAlias" /> FROM FILE_INDEX
        WHERE FILE_NAME = #{fileName,jdbcType=VARCHAR}
    </select>

</mapper>
```

---

## 13. Response 统一响应

```java
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 9:45
 * @Description 统一响应对象 - 所有API返回值统一封装
 *
 * ==================== 响应结构 ====================
 * {
 *   "code": "200",      // 状态码
 *   "msg": "成功",      // 消息
 *   "data": {...}       // 数据（可选）
 * }
 * ==================================================
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 2402460635136759519L;

    private String code;    // 状态码：2xx成功，4xx业务错误，5xx系统错误
    private String msg;     // 提示信息

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Object data;   // 响应数据

    // ==================== 静态工厂方法 ====================

    /** 成功（无数据） */
    public static Response success() {
        return new SuccessResponse();
    }

    /** 成功（有数据） */
    public static Response success(Object data) {
        return new SuccessResponse(data);
    }

    /** 失败（默认） */
    public static Response fail() {
        return new FailResponse();
    }

    /** 失败（自定义消息） */
    public static Response fail(String msg) {
        return new FailResponse(msg);
    }

    /** 参数错误 */
    public static Response paramError(String msg) {
        return new FailResponse(StatusEnum.PARAM_ERROR.getCode(), msg);
    }

    // ==================== 内部类实现 ====================

    /** 成功响应 */
    public static class SuccessResponse extends Response {
        public SuccessResponse() {
            this.setCode(StatusEnum.SUCCESS.getCode());
            this.setMsg(StatusEnum.SUCCESS.getMessage());
        }
        public SuccessResponse(Object data) {
            this.setCode(StatusEnum.SUCCESS.getCode());
            this.setMsg(StatusEnum.SUCCESS.getMessage());
            this.setData(data);
        }
    }

    /** 失败响应 */
    public static class FailResponse extends Response {
        public FailResponse() {
            this.setCode(StatusEnum.FAIL.getCode());
            this.setMsg(StatusEnum.FAIL.getMessage());
        }
        public FailResponse(String msg) {
            this.setCode(StatusEnum.FAIL.getCode());
            this.setMsg(msg);
        }
        public FailResponse(String code, String msg) {
            this.setCode(code);
            this.setMsg(msg);
        }
    }
}
```