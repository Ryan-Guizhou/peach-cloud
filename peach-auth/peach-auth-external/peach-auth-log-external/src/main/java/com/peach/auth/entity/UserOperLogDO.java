package com.peach.auth.entity;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * 模块编码。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/03/14 23:12
 */
@Data
@Table(name = "USER_OPER_LOG")
@Schema(description = "模块编码")
public class UserOperLogDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2118532517927027530L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键")
    private String id;

    @Column(name = "OPT_TYPE_CODE")
    @Schema(description = "操作类型编码")
    private String optTypeCode;

    @Column(name = "MODULE_CODE")
    @Schema(description = "模块编码")
    private String moduleCode;

    @Column(name = "CREATOR_CODE")
    @Schema(description = "创建人编码")
    private String creatorCode;

    @Column(name = "CREATOR_NAME")
    @Schema(description = "创建人名称")
    private String creatorName;

    @Column(name = "OPT_CONTENT")
    @Schema(description = "操作内容")
    private String optContent;

    @Column(name = "CREATE_TIME")
    @Schema(description = "创建时间")
    private String createTime;

    @Column(name = "OPT_LEVEL")
    @Schema(description = "操作级别")
    private String optLevel;

    @Column(name = "PRIVATE_IP")
    @Schema(description = "私网IP")
    private String privateIp;

    @Column(name = "PUBLIC_IP")
    @Schema(description = "公网IP")
    private String publicIp;

    @Column(name = "DEVICE")
    @Schema(description = "设备信息")
    private String device;

    @Column(name = "BROWSER")
    @Schema(description = "浏览器信息")
    private String browser;

    @Column(name = "OS")
    @Schema(description = "操作系统")
    private String os;

    @Column(name = "EXECUTION_TIME")
    @Schema(description = "执行时间（毫秒）")
    private Long executionTime;

    @Column(name = "IS_SUCCESS")
    @Schema(description = "是否成功 (Y: 成功, N: 失败)")
    private String isSuccess;

    @Column(name = "ERROR_MSG")
    @Schema(description = "错误信息")
    private String errorMsg;

    @Column(name = "RESPONSE_DATA")
    @Schema(description = "响应数据")
    private String responseData;

    @Column(name = "ROLE_CODE")
    @Schema(description = "角色编码")
    private String roleCode;

    @Column(name = "REQUEST_URI")
    @Schema(description = "请求路径")
    private String requestUri;

    @Column(name = "REQUEST_METHOD")
    @Schema(description = "请求方式")
    private String requestMethod;


}
