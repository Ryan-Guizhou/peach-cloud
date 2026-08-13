package com.peach.setting.dto;

import com.peach.setting.comon.enums.IpWhitelistGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * IP 白名单 DTO。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Data
public class IpWhitelistDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @NotBlank(groups = {IpWhitelistGroup.UpdatetGroup.class}, message = "IP白名单ID不能为空")
    private String id;

    @Schema(description = "IP地址")
    @NotBlank(groups = {IpWhitelistGroup.InsertGroup.class}, message = "IP地址不能为空")
    private String ipAddress;

    @Schema(description = "IP说明")
    private String ipDesc;

    @Schema(description = "状态，1启用，0禁用")
    private Integer status;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;
}
