package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前租户创建机构的业务参数。
 *
 * <p>租户和操作者身份不接受客户端指定；父机构必须是当前租户内的有效节点。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:00
 */
@Data
@Schema(description = "SaaS机构创建")
public class SaasOrganizationCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 219004L;

    @NotBlank(message = "机构编码不能为空")
    @Size(max = 64, message = "机构编码不能超过64个字符")
    private String orgCode;

    @NotBlank(message = "机构名称不能为空")
    @Size(max = 128, message = "机构名称不能超过128个字符")
    private String orgName;

    @Size(max = 32, message = "父机构物理ID不能超过32个字符")
    private String parentOrgRefId;
}
