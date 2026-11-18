package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 绑定到同一条有效成员角色授权及动作权限的字段规则。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
@Data
@Schema(description = "角色授权的字段读写效果")
public class ScopedFieldRuleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 882104726300554971L;

    private String bindingRefId;

    private String roleRefId;

    private String permissionRefId;

    private String fieldCode;

    private String maskStrategy;

    private String readEffect;

    private String writeEffect;
}
