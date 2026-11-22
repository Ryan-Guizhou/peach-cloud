package com.peach.auth.qo;
import com.peach.common.PeachEntity;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 租户管理。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantQO extends PeachEntity {
    @Size(max = 128)
    private String keyword;
    @Pattern(regexp = "PENDING|ACTIVE|SUSPENDED|CLOSED")
    private String status;
    @Size(max = 32)
    private String tenantType;
    @Override @NotNull @Min(1)
    public Integer getPageNum() { return super.getPageNum(); }
    @Override @NotNull @Min(1) @Max(200)
    public Integer getPageSize() { return super.getPageSize(); }
}
