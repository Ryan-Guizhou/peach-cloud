package com.peach.auth.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 角色工作区命令；作用域由服务端确定，revision防止覆盖他人的授权变更。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Data
public class RoleWorkspaceDTO {
    @NotBlank @Size(max = 32)
    private String roleId;
    @NotBlank @Size(max = 64)
    private String revision;
    @Size(max = 10000)
    private List<@NotBlank @Size(max = 128) String> funcCodes;
    @Size(max = 30000)
    private List<@NotBlank @Size(max = 500) String> resourceKeys;
    @Size(max = 10000)
    private List<@NotBlank @Size(max = 50) String> userCodes;
    @Size(max = 100)
    private String skipUrl;
}
