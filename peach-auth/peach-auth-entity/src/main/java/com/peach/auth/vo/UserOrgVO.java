package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.UserOrgDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户机构关系返回视图。
 * <p>用于登录后返回可切换机构列表和默认机构信息。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Data
@Schema(description = "用户机构关系统一返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserOrgVO extends UserOrgDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "机构编码")
    private String orgCode;

    @Schema(description = "机构名称")
    private String orgName;
}
