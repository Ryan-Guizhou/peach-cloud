package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新机构显示名称，不修改永久机构业务号及租户归属。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:00
 */
@Data
@Schema(description = "SaaS机构名称修改")
public class SaasOrganizationUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 219005L;

    @NotBlank(message = "机构名称不能为空")
    @Size(max = 128, message = "机构名称不能超过128个字符")
    private String orgName;
}
