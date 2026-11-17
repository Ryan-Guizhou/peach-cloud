package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单条角色绑定对特定语义权限形成的有效授权证据。
 *
 * <p>同一条证据同时包含动作所归属角色和作用域，不允许跨证据拼接权限动作与范围。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
@Data
@Schema(description = "成员单条角色授权证据")
public class RoleScopeGrantVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -799671108601132978L;

    private String bindingRefId;

    private String roleRefId;

    private String scopeType;

    private String scopeRefId;

    private String permissionCode;
}
