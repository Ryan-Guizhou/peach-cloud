package com.peach.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
 */
@Data
@Entity
@Table(name = "PEACH_AUTH_LOG")
@Schema(description = "AuthLog实体")
public class AuthLogDO  implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "LOG_ID")
    @Schema(description = "日志ID")
    private String logId;

    @Column(name = "TENANT_ID")
    @Schema(description = "租户ID")
    private String tenantId;

    @Column(name = "ORG_ID")
    @Schema(description = "机构ID")
    private String orgId;

    @Column(name = "OPERATOR_USER_ID")
    @Schema(description = "操作人用户ID")
    private String operatorUserId;

    @Column(name = "OPERATOR_CODE")
    @Schema(description = "操作人账号")
    private String operatorCode;

    @Column(name = "OPERATOR_NAME")
    @Schema(description = "操作人名称")
    private String operatorName;

    @Column(name = "USER_CODE")
    @Schema(description = "被授权用户账号")
    private String userCode;

    @Column(name = "USER_ID")
    @Schema(description = "被授权用户ID")
    private String userId;

    @Column(name = "USER_NAME")
    @Schema(description = "被授权用户名称")
    private String userName;

    @Column(name = "AUTH_DESCRIBE")
    @Schema(description = "授权描述")
    private String authDescribe;

    @Column(name = "OPERAT_TIME")
    @Schema(description = "操作时间")
    private String operatTime;


}
