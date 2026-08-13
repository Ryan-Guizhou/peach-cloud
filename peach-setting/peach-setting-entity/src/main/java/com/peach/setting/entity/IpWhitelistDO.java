package com.peach.setting.entity;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * IP 白名单。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 00:00
 */
@Data
@Entity
@Table(name = "PEACH_IP_WHITELIST")
@Schema(description = "IP白名单")
@EqualsAndHashCode(callSuper = true)
public class IpWhitelistDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "IP_ADDRESS")
    @Schema(description = "IP地址")
    private String ipAddress;

    @Column(name = "IP_DESC")
    @Schema(description = "IP说明")
    private String ipDesc;

    @Column(name = "STATUS")
    @Schema(description = "状态，1启用，0禁用")
    private Integer status;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;
}
