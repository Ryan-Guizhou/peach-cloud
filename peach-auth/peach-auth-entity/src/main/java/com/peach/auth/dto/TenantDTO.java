package com.peach.auth.dto;
import com.peach.auth.group.TenantGroup;
import jakarta.validation.constraints.*;
import lombok.Data;
/**
 * 租户管理。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
@Data
public class TenantDTO {
    @NotBlank(groups = TenantGroup.Update.class)
    @Size(max = 36)
    private String id;
    @NotNull(groups = TenantGroup.Update.class)
    @PositiveOrZero
    private Long version;
    @NotBlank
    @Size(max = 128)
    private String tenantCode;
    @NotBlank
    @Size(max = 128)
    private String tenantName;
    @NotBlank
    @Size(max = 32)
    private String tenantType;
    @Size(max = 500)
    private String description;
    @Size(max = 64)
    private String contactName;
    @Size(max = 32)
    private String contactPhone;
    @Email
    @Size(max = 128)
    private String contactEmail;
}
