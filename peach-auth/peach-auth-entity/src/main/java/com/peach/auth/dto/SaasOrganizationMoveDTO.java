package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 机构移动的目标物理父节点；null 表示移动为根机构。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:00
 */
@Data
@Schema(description = "SaaS机构移动")
public class SaasOrganizationMoveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 219006L;

    @Size(max = 32, message = "父机构物理ID不能超过32个字符")
    private String newParentOrgRefId;
}
