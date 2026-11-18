package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前成员在同一条角色绑定中可使用的行范围授权证据。
 *
 * <p>一行同时包含角色绑定、动作权限与数据策略，不允许跨角色拼接动作和机构范围。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
@Data
@Schema(description = "角色单绑定的行范围授权证据")
public class RoleDataGrantVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -219897712343556211L;

    private String bindingRefId;

    private String roleRefId;

    private String permissionRefId;

    private String bindingScopeType;

    private String bindingOrgRefId;

    private String dataScope;

    private String dataOrgRefId;
}
