package com.peach.auth.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户操作日志查询参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 22:50
  */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户操作日志查询参数")
public class UserOperLogQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1133878252981634479L;

    @Schema(description = "操作类型编码")
    private String optTypeCode;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "创建人编码")
    private String creatorCode;

    @Schema(description = "操作级别")
    private String optLevel;

    @Schema(description = "是否成功")
    private String isSuccess;

    @Schema(description = "请求路径")
    private String requestUri;

    @Schema(description = "请求方式")
    private String requestMethod;
}
