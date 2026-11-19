package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务端从文件数据库真实记录构造的受信行级授权事实。
 *
 * <p>只能在通过 Same-Token 的受信服务间使用，不允许浏览器自报对象权属。
 * 文件动作采用有限语义码，禁止传入 SQL、租户条件、角色 ID 或任意表达式。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Data
@Schema(description = "受信文件对象授权校验参数")
public class SaasIamFileObjectCheckDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 232001L;

    @NotBlank
    @Pattern(regexp = "FILE\\.(LIST|READ|DOWNLOAD|UPLOAD|DELETE|RESTORE|EDIT)")
    private String permissionCode;

    @NotBlank
    @Size(max = 32)
    private String tenantRefId;

    @NotBlank
    @Size(max = 32)
    private String appRefId;

    @NotBlank
    @Size(max = 32)
    private String objectRefId;

    @NotBlank
    @Size(max = 32)
    private String ownerUserRefId;

    @Size(max = 32)
    private String orgRefId;

    private boolean deleted;
}
