package com.peach.auth.entity;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
 */
@Data
@Entity
@Table(name = "PEACH_RESOURCE")
@Schema(description = "Resource实体")
@EqualsAndHashCode(callSuper = true)
public class ResourceDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "RESOURCE_ID")
    @Schema(description = "资源ID")
    private String resourceId;

    @Column(name = "FUNC_CODE")
    @Schema(description = "功能编码")
    private String funcCode;

    @Column(name = "RESOURCE_TYPE")
    @Schema(description = "资源类型")
    private String resourceType;

    @Column(name = "RESOURCE_CODE")
    @Schema(description = "资源编码")
    private String resourceCode;

    @Column(name = "RESOURCE_NAME")
    @Schema(description = "资源名称")
    private String resourceName;

    @Column(name = "APP_ID")
    @Schema(description = "应用ID")
    private String appId;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

}
