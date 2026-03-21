# Peach 后端代码风格规范 - 错误示例

本文档展示违反 Peach 后端代码规范的错误写法，并给出正确示例。

---

## 1. DO 实体类 - 常见错误

### ❌ 错误：缺少 serialVersionUID

```java
import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 错误示例 - 缺少serialVersionUID
 */
@Data
@Entity
@Table(name = "FILE_INDEX")
public class FileIndexDO implements Serializable {
    // ❌ 错误：未定义 serialVersionUID
    // 反序列化时可能导致 InvalidClassException

    @Id
    @Column(name = "主键")
    private String id;
}
```

### ✅ 正确写法

```java
public class FileIndexDO implements Serializable {

    private static final long serialVersionUID = 1967646335443236467L;  // ✅ 必须定义

    // ...
}
```

---

## 2. DO 实体类 - 常见错误

### ❌ 错误：使用 Java 9+ 语法

```java
import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:59
 * @Description 错误示例 - 使用Java 9+语法
 */
@Data
@Entity
@Table(name = "FILE_INDEX")
public class FileIndexDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "主键")
    private String id;

    // ❌ 错误：使用了 record（Java 14+ 特性）
    // ❌ 错误：项目仅支持 JDK 1.8
    public record FileIndexRecord(String id, String name) {}
}
```

### ✅ 正确写法

```java
// ✅ 使用普通类代替 record
public class FileIndexDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    // getter/setter...
}
```

---

## 3. Controller - 常见错误

### ❌ 错误：缺少必要注解

```java
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 错误示例 - Controller缺少必要注解
 */
// ❌ 错误：@Tag - Swagger文档分组（必须）
// ❌ 错误：@Slf4j - 日志输出（必须）
// ❌ 错误：@Indexed - 性能优化（必须）
// ❌ 错误：@Operation - 接口描述（必须）
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    // ❌ 错误：Service注入使用@Autowired（业务Bean应用@Resource）
    @Autowired
    private FileService fileService;

    // ❌ 错误：方法缺少@Operation注解
    @PostMapping("/queryPage")
    public Response queryPage(@RequestBody FileIndexQO queryQO) {
        return fileService.queryPage(queryQO);
    }
}
```

### ✅ 正确写法

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

// ✅ 正确：Controller必须包含以下注解
@Tag(name = "文件管理", description = "文件索引管理")
@Slf4j
@Indexed
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    // ✅ 正确：业务Bean使用@Resource注入
    @Resource
    private IFileService fileService;

    @Operation(summary = "分页查询文件")
    @PostMapping("/queryPage")
    public Response queryPage(@RequestBody FileIndexQO queryQO) {
        return fileService.queryPage(queryQO);
    }
}
```

---

## 4. Controller - 常见错误

### ❌ 错误：使用 System.out.println

```java
@Tag(name = "文件管理")
@Slf4j
@RestController
public class FileController {

    @Operation(summary = "查询文件")
    @PostMapping("/queryPage")
    public Response queryPage(@RequestBody FileIndexQO queryQO) {
        // ❌ 错误：使用System.out.println（禁止）
        System.out.println("查询文件：" + queryQO);

        // ❌ 错误：吞掉异常不记录
        try {
            // 业务逻辑
        } catch (Exception e) {
            // ❌ 错误：没有记录日志和堆栈
        }

        return fileService.queryPage(queryQO);
    }
}
```

### ✅ 正确写法

```java
@Tag(name = "文件管理")
@Slf4j
@RestController
public class FileController {

    @Operation(summary = "查询文件")
    @PostMapping("/queryPage")
    public Response queryPage(@RequestBody FileIndexQO queryQO) {
        // ✅ 正确：使用@Slf4j日志
        log.info("分页查询文件, param: {}", queryQO);

        try {
            // 业务逻辑
            return fileService.queryPage(queryQO);
        } catch (Exception e) {
            // ✅ 正确：记录异常日志和堆栈信息
            log.error("查询文件失败, param: {}, error: {}", queryQO, e.getMessage(), e);
            return Response.fail("查询失败");
        }
    }
}
```

---

## 5. Controller - 常见错误

### ❌ 错误：Controller 写业务逻辑

```java
@Tag(name = "文件管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Resource
    private FileDao fileDao;

    @Operation(summary = "保存文件")
    @PostMapping("/saveFile")
    public Response saveFile(@RequestBody FileIndexDTO dto) {
        // ❌ 错误：Controller 直接操作数据库（违反分层架构）
        FileIndexDO fileIndexDO = new FileIndexDO();
        fileIndexDO.setId(UUID.randomUUID().toString());
        fileIndexDO.setFileName(dto.getFileName());
        fileIndexDO.setFilePath(dto.getFilePath());
        fileIndexDO.setCreateTime(new Date());
        fileDao.insert(fileIndexDO);  // ❌ 错误：Controller不应调用DAO

        return Response.success();
    }
}
```
### ✅ 正确写法

```java
@Tag(name = "文件管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Resource
    private IFileService fileService;  // ✅ 正确：Controller只调用Service

    @Operation(summary = "保存文件")
    @PostMapping("/saveFile")
    public Response saveFile(@RequestBody FileIndexDTO dto) {
        // ✅ 正确：业务逻辑放在Service层
        return fileService.saveFile(dto);
    }
}
```

---
## 6. Controller - 常见错误

### ❌ 错误：Controller 高危操作没有加日志

```java
@Tag(name = "文件管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    
    @Resource
    private IFileService iFileService;

    @Operation(summary = "保存文件")
    @PostMapping("/saveFile")
//    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
//            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'新增索引信息:['+#p0+']'") // ❌ 错误：Controller 高危操作需要加日志 使用spel表达式，日志信息需要记录重要的信息 如果分不清则全部记录
    public Response saveFile(@RequestBody FileIndexDTO dto) {
        FileIndexDO fileIndexDO = new FileIndexDO();
        fileIndexDO.setId(UUID.randomUUID().toString());
        fileIndexDO.setFileName(dto.getFileName());
        fileIndexDO.setFilePath(dto.getFilePath());
        fileIndexDO.setCreateTime(new Date());
        iFileService.insert(fileIndexDO);  

        return Response.success();
    }
}
```
### ✅ 正确写法

```java
@Tag(name = "文件管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Resource
    private IFileService iFileService;

    @Operation(summary = "保存文件")
    @PostMapping("/saveFile")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'新增索引信息:['+#p0+']'") 
    public Response saveFile(@RequestBody FileIndexDTO dto) {
        FileIndexDO fileIndexDO = new FileIndexDO();
        fileIndexDO.setId(UUID.randomUUID().toString());
        fileIndexDO.setFileName(dto.getFileName());
        fileIndexDO.setFilePath(dto.getFilePath());
        fileIndexDO.setCreateTime(new Date());
        iFileService.insert(fileIndexDO);

        return Response.success();
    }

    @Operation(summary = "保存文件")
    @PostMapping("/deleteById")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除索引信息,索引ID为:['+#p0+']'")
    public Response saveFile(@PathVariable String indexId) {
        FileIndexDO fileIndexDO = new FileIndexDO();
        fileIndexDO.setId(UUID.randomUUID().toString());
        fileIndexDO.setFileName(dto.getFileName());
        fileIndexDO.setFilePath(dto.getFilePath());
        fileIndexDO.setCreateTime(new Date());
        iFileService.insert(fileIndexDO);

        return Response.success();
    }
}
```


---

## 6. Controller - 方法命名错误

### ❌ 错误：Controller 方法命名不符合规范

```java
@Tag(name = "文件管理")
@Slf4j
@Indexed
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Resource
    private IFileService iFileService;

    // ❌ 错误：方法必须以 query/save/modify/delete 开头
    @Operation(summary = "分页查询文件")
    @PostMapping("/queryPage")
    public Response queryPageFiles(@RequestBody FileQueryQO qo) {
        return iFileService.queryPage(qo);
    }

    // ❌ 错误：更新方法必须以 modify 开头，不能用 update
    @Operation(summary = "更新文件")
    @PostMapping("/updateById")
    public Response updateById(@RequestBody FileIndexDTO dto) {
        return iFileService.updateById(dto);
    }

    // ❌ 错误：save/modify/delete 方法必须添加 @UserOperLog 注解
    @Operation(summary = "保存文件")
    @PostMapping("/save")
    public Response addFile(@RequestBody FileIndexDTO dto) {
        return iFileService.saveFile(dto);
    }

    // ❌ 错误：删除方法必须以 delete 开头
    @Operation(summary = "删除文件")
    @PostMapping("/remove")
    public Response removeFile(@PathVariable String id) {
        return iFileService.deleteById(id);
    }
}
```

### ✅ 正确写法

```java
@Tag(name = "文件管理")
@Slf4j
@Indexed
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Resource
    private IFileService iFileService;

    // ✅ 正确：查询方法以 query 开头
    @Operation(summary = "分页查询文件")
    @PostMapping("/queryPage")
    public Response queryPage(@RequestBody FileQueryQO qo) {
        return iFileService.queryPage(qo);
    }

    // ✅ 正确：更新方法以 modify 开头
    @Operation(summary = "更新文件")
    @PostMapping("/modifyById")
    @UserOperLog(
        moduleCode = UserLogEnum.Module.USERSERVICE,
        optType = UserLogEnum.OptType.UPDATE,
        optLevel = UserLogEnum.LogLevel.DEBUG,
        optContent = "'更新文件ID:['+#p0.id+']'"
    )
    public Response modifyById(@RequestBody FileIndexDTO dto) {
        return iFileService.updateById(dto);
    }

    // ✅ 正确：保存方法以 save 开头，且添加 @UserOperLog
    @Operation(summary = "保存文件")
    @PostMapping("/save")
    @UserOperLog(
        moduleCode = UserLogEnum.Module.USERSERVICE,
        optType = UserLogEnum.OptType.INSERT,
        optLevel = UserLogEnum.LogLevel.INFO,
        optContent = "'保存文件:['+#p0.fileName+']'"
    )
    public Response saveFile(@RequestBody FileIndexDTO dto) {
        return iFileService.saveFile(dto);
    }

    // ✅ 正确：删除方法以 delete 开头，且添加 @UserOperLog
    @Operation(summary = "删除文件")
    @DeleteMapping("/delete/{id}")
    @UserOperLog(
        moduleCode = UserLogEnum.Module.USERSERVICE,
        optType = UserLogEnum.OptType.DELETE,
        optLevel = UserLogEnum.LogLevel.WARN,
        optContent = "'删除文件ID:['+#p0+']'"
    )
    public Response deleteById(@PathVariable String id) {
        return iFileService.deleteById(id);
    }
}
```

---

---
## 7. Controller - 常见错误

### ❌ 错误：Controller 高危操作没有加日志

```java
@Tag(name = "文件管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    
    @Resource
    private IFileService iFileService;

    @Operation(summary = "保存文件")
    @PostMapping("/saveFile")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'新增索引信息:['+#p0+']'") 
    public Response saveFile(
//            @Validated(FileIndexGroup.insertGroup.class) ❌ 错误：Controller 如果接收参数的对象使用参数校验，需要增加对应的参数分组校验
            @RequestBody FileIndexDTO dto) {
        FileIndexDO fileIndexDO = new FileIndexDO();
        fileIndexDO.setId(UUID.randomUUID().toString());
        fileIndexDO.setFileName(dto.getFileName());
        fileIndexDO.setFilePath(dto.getFilePath());
        fileIndexDO.setCreateTime(new Date());
        iFileService.insert(fileIndexDO);  

        return Response.success();
    }
}
```
### ✅ 正确写法

```java
@Tag(name = "文件管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    @Resource
    private IFileService iFileService;

    @Operation(summary = "保存文件")
    @PostMapping("/saveFile")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'新增索引信息:['+#p0+']'") 
    public Response saveFile(@Validated(FileIndexGroup.insertGroup.class)  @RequestBody FileIndexDTO dto) {
        FileIndexDO fileIndexDO = new FileIndexDO();
        fileIndexDO.setId(UUID.randomUUID().toString());
        fileIndexDO.setFileName(dto.getFileName());
        fileIndexDO.setFilePath(dto.getFilePath());
        fileIndexDO.setCreateTime(new Date());
        iFileService.insert(fileIndexDO);

        return Response.success();
    }
    
}
```


---

## 8. Service - 常见错误

### ❌ 错误：Service 缺少必要注解

```java
// ❌ 错误：缺少@Slf4j（必须）
// ❌ 错误：缺少@Indexed（必须）
@Service
public class FileServiceImpl implements IFileService {

    // ❌ 错误：业务DAO使用@Autowired（应用@Resource）
    @Autowired
    private FileDao fileDao;

    @Override
    public Response queryPage(FileIndexQO queryQO) {
        return null;
    }
}
```

### ✅ 正确写法

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

// ✅ 正确：ServiceImpl必要注解
@Slf4j
@Indexed
@Service
public class FileServiceImpl implements IFileService {

    // ✅ 正确：业务Bean使用@Resource注入
    @Resource
    private FileDao fileDao;

    @Override
    public Response queryPage(FileIndexQO queryQO) {
        log.info("分页查询文件");
        return null;
    }
}
```

---

## 9. DAO - 常见错误

### ❌ 错误：缺少必要注解

```java
import com.peach.common.PeachDao;
import com.peach.auth.entity.FileIndexDO;
import com.peach.auth.vo.FileIndexVO;

// ❌ 错误：@Indexed（必须）
// ❌ 错误：@MybatisDao（必须）
public interface FileIndexDao extends PeachDao<FileIndexDO, FileIndexVO> {
    // ...
}
```

### ✅ 正确写法

```java
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import org.springframework.stereotype.Indexed;

// ✅ 正确：DAO必要注解
@Indexed
@MybatisDao
public interface FileIndexDao extends PeachDao<FileIndexDO, FileIndexVO> {
    // ...
}
```

---

## 10. MyBatis XML - 常见错误

### ❌ 错误：使用 SELECT *

```xml
<!-- ❌ 错误：使用 SELECT *（禁止） -->
<select id="selectById" parameterType="string" resultType="com.peach.auth.vo.FileIndexVO">
    SELECT * FROM FILE_INDEX WHERE ID = #{value}
</select>

<!-- ❌ 错误：namespace 为空 -->
<mapper namespace="">
    ...
</mapper>
```

### ✅ 正确写法

```xml
<!-- ✅ 正确：指定具体列名，使用SQL片段 -->
<mapper namespace="com.peach.auth.dao.FileIndexDao">

    <sql id="allColumnAlias">
        ID as id, FILE_NAME as fileName, FILE_PATH as filePath
    </sql>

    <select id="selectById" parameterType="string" resultType="com.peach.auth.vo.FileIndexVO">
        SELECT <include refid="allColumnAlias" />
        FROM FILE_INDEX
        WHERE ID = #{value,jdbcType=VARCHAR}
    </select>
</mapper>
```

---

## 11. MyBatis XML - 常见错误

### ❌ 错误：缺少 jdbcType

```xml
<!-- ❌ 错误：参数未指定jdbcType（可能引发异常） -->
<insert id="insert" parameterType="com.peach.auth.entity.FileIndexDO">
    INSERT INTO FILE_INDEX (ID, FILE_NAME)
    VALUES (#{id}, #{fileName})  <!-- ❌ 缺少 jdbcType -->
</insert>

<!-- ❌ 错误：条件判断使用 != null 但未判断空字符串 -->
<sql id="allColumnCond">
    <if test="fileName != null">  <!-- ❌ 字符串类型应同时判断非空 -->
        AND FILE_NAME = #{fileName}
    </if>
</sql>
```

### ✅ 正确写法

```xml
<!-- ✅ 正确：所有参数必须指定 jdbcType -->
<insert id="insert" parameterType="com.peach.auth.entity.FileIndexDO">
    INSERT INTO FILE_INDEX (ID, FILE_NAME)
    VALUES (#{id,jdbcType=VARCHAR}, #{fileName,jdbcType=VARCHAR})
</insert>

<!-- ✅ 正确：字符串类型需判断 null 和空字符串 -->
<sql id="allColumnCond">
    <if test="fileName != null and fileName != ''">
        AND FILE_NAME = #{fileName,jdbcType=VARCHAR}
    </if>
</sql>
```

---

## 12. DTO/QO - 常见错误

### ❌ 错误：缺少分组校验

```java
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 错误示例 - DTO缺少分组校验规则
 */
@Data
@Schema(description = "文件索引保存对象")
public class FileIndexDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ❌ 错误：未区分新增/更新的校验规则
    // ❌ 错误：新增时允许传入ID，更新时ID可能为空
    @Schema(description = "主键ID")
    private String id;  // 新增时应为空，更新时应有值

    @Schema(description = "文件名称")
    private String fileName;  // 新增和更新都应该校验不为空
}
```

### ✅ 正确写法

```java
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * 正确示例 - DTO使用分组校验
 */
@Data
@Schema(description = "文件索引保存对象")
public class FileIndexDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ✅ 正确：新增时ID必须为空，更新时ID必须有值
    @Schema(description = "主键ID")
    @NotNull(message = "更新时ID不能为空", groups = FileIndexGroup.updateGroup.class)
    @Null(message = "新增时不可指定ID", groups = FileIndexGroup.insertGroup.class)
    private String id;

    // ✅ 正确：必填字段在新增和更新时都需要校验
    @Schema(description = "文件名称", requiredMode = REQUIRED)
    @NotBlank(message = "文件名不能为空", groups = {FileIndexGroup.insertGroup.class, FileIndexGroup.updateGroup.class})
    private String fileName;
}

// 分组定义
class FileIndexGroup {
    interface insertGroup {}
    interface updateGroup {}
}
```

---

## 13. 常量/枚举 - 常见错误

### ❌ 错误：硬编码魔法值

```java
public class FileServiceImpl {

    public void saveFile(FileIndexDTO dto) {
        // ❌ 错误：硬编码魔法值（应使用常量/枚举）
        dto.setStatus(1);  // 1是什么？无法理解

        // ❌ 错误：使用字符串硬编码
        if ("yes".equals(dto.getIsEnable())) {
            // ...
        }

        // ❌ 错误：数字类型状态没有枚举
        if (dto.getFileType() == 1) {
            // ...
        }
    }
}
```

### ✅ 正确写法

```java
// 定义状态枚举
public enum FileStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布");

    private final Integer code;
    private final String desc;

    FileStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}

// 使用常量
public class FileConstants {
    public static final String DEFAULT_PATH = "/data/files/";
    public static final Integer MAX_FILE_SIZE = 100 * 1024 * 1024;
}

public class FileServiceImpl {

    public void saveFile(FileIndexDTO dto) {
        // ✅ 正确：使用枚举
        dto.setStatus(FileStatusEnum.PUBLISHED.getCode());

        // ✅ 正确：使用常量
        dto.setFilePath(FileConstants.DEFAULT_PATH);
    }
}
```

---

## 14. 命名规范 - 常见错误

### ❌ 错误：命名不符合规范

```java
// ❌ 错误：Controller命名不符合规范
public class fileController {}           // 应为 FileController
public class FileController {}           // ✅ 正确

// ❌ 错误：Service接口缺少I前缀
public interface FileService {}          // 应为 IFileService

// ❌ 错误：ServiceImpl命名不符合规范
public class FileService implements IFileService {}  // 应为 FileServiceImpl

// ❌ 错误：DO/VO/DTO/QO后缀缺失
public class FileIndex {}                // 应为 FileIndexDO
public class FileIndexView {}           // 应为 FileIndexVO

// ❌ 错误：方法命名不符合规范（应使用小驼峰）
void SelectById();                       // 应为 selectById
void SaveFile();                         // 应为 saveFile
```

### ✅ 正确写法

```java
// Controller
public class FileController {}

// Service 接口（I前缀）
public interface IFileService {}

// Service 实现（Impl后缀）
public class FileServiceImpl implements IFileService {}

// 实体类（正确后缀）
public class FileIndexDO {}      // 持久化对象
public class FileIndexVO {}      // 视图对象
public class FileIndexQO {}      // 查询参数
public class FileIndexDTO {}     // 数据传输对象

// 方法命名（小驼峰动词开头）
void selectById();
void saveFile();
void queryPage();
```

---

## 15. 异常处理 - 常见错误

### ❌ 错误：吞异常

```java
public class FileServiceImpl {

    public void processFile(String fileId) {
        try {
            FileIndexDO file = fileDao.selectById(fileId);
            // 业务处理
        } catch (Exception e) {
            // ❌ 错误：吞异常 - 最糟糕的做法
        }

        try {
            // 业务处理
        } catch (Exception e) {
            // ❌ 错误：只打印日志不抛出异常（可能影响调用方判断）
            log.error("error: {}", e.getMessage());
        }
    }
}
```

### ✅ 正确写法

```java
public class FileServiceImpl {

    public void processFile(String fileId) {
        try {
            FileIndexDO file = fileDao.selectById(fileId);
            if (file == null) {
                // ✅ 正确：业务异常应返回特定错误码
                log.warn("文件不存在, fileId: {}", fileId);
                return Response.fail("文件不存在");
            }
            // 业务处理
        } catch (Exception e) {
            // ✅ 正确：记录完整日志+堆栈+抛出异常
            log.error("处理文件异常, fileId: {}, error: {}", fileId, e.getMessage(), e);
            throw new BusinessException("处理文件失败，请稍后重试");
        }
    }
}
```

---

## 16. 跨模块依赖 - 常见错误

### ❌ 错误：跨模块重复造轮子

```java
package com.peach.auth.common;  // ❌ 错误：不应在auth模块重复定义common

// ❌ 错误：copystrings等工具方法已在peach-common中定义
public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
```

### ✅ 正确写法

```java
// ✅ 正确：使用peach-common中的工具类
import com.peach.common.util.StringUtil;

// ✅ 正确：需要枚举应在common模块定义或引用现有枚举
import com.peach.common.enums.StatusEnum;
```

---

## 16. 跨模块依赖 - 常见错误

### ❌ 错误：跨模块重复造轮子

```java
package com.peach.common;  // ❌ 错误：不应在auth模块重复定义common

// ❌ 错误：copystrings等工具方法已在peach-common中定义
public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
```

### ✅ 正确写法

```java
// ✅ 正确：使用peach-common中的工具类
import com.peach.common.util.StringUtil;

// ✅ 正确：需要枚举应在common模块定义或引用现有枚举
import com.peach.common.enums.StatusEnum;
```
---

## 17. 主键规范 - 常见错误

### ❌ 错误：使用自增主键

```java
import lombok.Data;
import javax.persistence.*;

/**
 * 错误示例 - DO 主键使用自增（禁止
 */
@Data
@Entity
@Table(name = "FILE_INDEX")
public class FileIndexDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ❌ 错误：使用自增主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ❌ 禁止使用自增
    @Column(name = "主键")
    private Long id;  // ❌ 禁止使用 Long/Integer 类型

    // ❌ 错误：使用 Date/LocalDateTime 类型
    @Column(name = "创建时间")
    private Date createTime;  // ❌ 禁止使用 Date 类型

    @Column(name = "创建时间")
    private LocalDateTime createTime2;  // ❌ 禁止使用 LocalDateTime

}

### ✅ 正确写法

```java
// ✅ 正确：使用 String 类型（UUID）
public class FileIndexDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    // ✅ 正确：使用 String 类型主键
    @Id
    @Column(name = "主键，length = 36）
    private String id;

    // ✅ 正确：String 类型时间字段
    private String createTime;
}
```

---

## 18. 时间字段规范 - 常见错误

### ❌ 错误：使用 Date 类型时间字段

```java
import java.util.Date;

// ❌ 错误：使用 Date 类型

public class FileIndexDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "创建时间")
    private Date createTime;  // ❌ 错误
}
```

### ✅ 正确写法

```java
// ✅ 正确：使用 String 类型
private String createTime;
```

---

## 总结：自检清单

| 检查项                       | 说明                                 |
|---------------------------|------------------------------------|
| ✅ serialVersionUID        | 所有 Serializable 类必须定义              |
| ✅ Java 8 语法               | 禁止 var/record/List.of 等            |
| ✅ @Slf4j                  | Controller/Service 必须有日志           |
| ✅ @Indexed                | Controller/Service/DAO 必须有         |
| ✅ @Tag/@Operation         | Controller 必须有                     |
| ✅ @MybatisDao             | DAO 必须有                            |
| ✅ @Resource vs @Autowired | 业务Bean用@Resource，框架Bean用@Autowired |
| ✅ @UserOperLog            | 所有高位操作都需要添加 新增 / 删除 / 修改           |
| ✅ 分组校验                    | DTO 区分 insert/update 校验规则          |
| ✅ 禁止 SELECT *             | MyBatis XML 必须指定列名                 |
| ✅ jdbcType                | MyBatis 参数必须指定 jdbcType            |
| ✅ 禁止硬编码                   | 使用常量/枚举替代魔法值                       |
| ✅ 禁止吞异常                   | 必须记录日志并抛出或返回错误                     |
| ✅ 主键类型必须是String（UUID） | 禁止使用 Long + 自增                        |
| ✅ 时间字段必须是 String          | 禁止使用 Date/LocalDateTime               |
---

## 12. 参数校验规范

### ❌ 错误：Controller 使用 @Validated 但 QO/DTO 没有校验规则

```java
// QO 类 - 没有定义任何校验规则
@Data
@Schema(description = "用户查询参数")
public class UserQO extends PeachEntity {
    private String username;
    private String phone;
}

// Controller - 无需使用 @Validated
@Operation(summary = "查询用户列表")
@PostMapping("/queryPage")
public Response queryPage(@Validated @RequestBody UserQO qo) {  // ❌ 错误：QO 没有校验规则却使用了 @Validated
    return userService.queryPage(qo);
}
```

### ❌ 错误：QO/DTO 有校验规则但 Controller 没有使用 @Validated

```java
// DTO 类 - 定义了校验规则
@Data
@Schema(description = "用户保存DTO")
public class UserDTO implements Serializable {
    @NotBlank(message = "用户名不能为空", groups = PeachGroup.insertGroup.class)
    private String username;

    @NotBlank(message = "密码不能为空", groups = PeachGroup.insertGroup.class)
    @Size(min = 6, max = 20, message = "密码长度6-20位", groups = PeachGroup.insertGroup.class)
    private String password;
}

// Controller - ❌ 错误：DTO 有校验规则但没有使用 @Validated
@Operation(summary = "保存用户")
@PostMapping("/save")
public Response saveUser(@RequestBody UserDTO dto) {  // 缺少 @Validated
    return userService.saveUser(dto);
}
```

### ✅ 正确写法

```java
// 1. QO 类 - 不需要校验规则时，Controller 也不需要 @Validated
@Data
@Schema(description = "用户查询参数")
public class UserQO extends PeachEntity {
    private String username;
    private String phone;
}

// Controller - 不需要 @Validated
@Operation(summary = "查询用户列表")
@PostMapping("/queryPage")
public Response queryPage(@RequestBody UserQO qo) {
    return userService.queryPage(qo);
}

// 2. DTO 类 - 有校验规则时，Controller 需要配合 @Validated
@Data
@Schema(description = "用户保存DTO")
public class UserDTO implements Serializable {
    @NotBlank(message = "用户名不能为空", groups = PeachGroup.insertGroup.class)
    private String username;

    @NotBlank(message = "密码不能为空", groups = PeachGroup.insertGroup.class)
    @Size(min = 6, max = 20, message = "密码长度6-20位", groups = PeachGroup.insertGroup.class)
    private String password;

    @NotNull(message = "更新时ID不能为空", groups = PeachGroup.updateGroup.class)
    private String userId;
}

// Controller - 使用 @Validated 并指定分组
@Operation(summary = "保存用户")
@PostMapping("/save")
@UserOperLog(
    moduleCode = UserLogEnum.Module.USERSERVICE,
    optType = UserLogEnum.OptType.INSERT,
    optLevel = UserLogEnum.LogLevel.INFO,
    optContent = "'新增用户:['+#p0.username+']'"
)
public Response saveUser(@Validated(PeachGroup.insertGroup.class) @RequestBody UserDTO dto) {
    return userService.saveUser(dto);
}

@Operation(summary = "更新用户")
@PostMapping("/modify")
@UserOperLog(
    moduleCode = UserLogEnum.Module.USERSERVICE,
    optType = UserLogEnum.OptType.UPDATE,
    optLevel = UserLogEnum.LogLevel.DEBUG,
    optContent = "'更新用户ID:['+#p0.userId+']'"
)
public Response modifyUser(@Validated(PeachGroup.updateGroup.class) @RequestBody UserDTO dto) {
    return userService.updateUser(dto);
}
```

### 📋 校验规范总结

| 场景 | QO/DTO 校验规则 | Controller @Validated |
|------|----------------|---------------------|
| QO 无校验规则 | 不需要 | 不需要 |
| QO 有校验规则 | 需要 `@NotNull` 等 | 需要 `@Validated(Group.class)` |
| DTO 新增 | 需要 `@Null` + 字段校验 | 需要 `@Validated(PeachGroup.insertGroup.class)` |
| DTO 更新 | 需要 `@NotNull` + 字段校验 | 需要 `@Validated(PeachGroup.updateGroup.class)` |
| DTO 查询 | 需要字段校验 | 需要 `@Validated(Group.class)` |

**注意**：分组校验规则定义在 DTO 中：
- `insertGroup`：新增时 ID 必须为 `@Null`
- `updateGroup`：更新时 ID 必须为 `@NotNull`
