package com.peach.setting.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * IP白名单查询对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "IP白名单查询对象")
public class IpWhitelistQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -6815161342677099102L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "IP地址")
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
