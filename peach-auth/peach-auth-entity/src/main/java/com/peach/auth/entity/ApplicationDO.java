package com.peach.auth.entity;

import java.io.Serial;

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
 * PeachApplication实体。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
  */
@Data
@Entity
@Table(name = "PEACH_APPLICATION")
@Schema(description = "PeachApplication实体")
@EqualsAndHashCode(callSuper = true)
public class ApplicationDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1860066138968904427L;

    @Id
    @Column(name = "APP_ID")
    @Schema(description = "应用ID")
    private String appId;

    @Column(name = "APP_NAME")
    @Schema(description = "应用名称")
    private String appName;

    @Column(name = "APP_TYPE")
    @Schema(description = "应用类型")
    private String appType;

    @Column(name = "IS_OPEN")
    @Schema(description = "是否启用")
    private Integer isOpen;

    @Column(name = "APP_DESC")
    @Schema(description = "应用描述")
    private String appDesc;

    @Column(name = "LOGOUT_URL")
    @Schema(description = "退出URL")
    private String logoutUrl;

    @Column(name = "SORT_NUM")
    @Schema(description = "显示顺序")
    private String sortNum;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;


}
