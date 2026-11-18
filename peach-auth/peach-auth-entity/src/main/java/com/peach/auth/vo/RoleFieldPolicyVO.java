package com.peach.auth.vo;

import lombok.Data;

/**
 * 已存储的角色动作字段策略快照，用于事务差量判断。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:45
 */
@Data
public class RoleFieldPolicyVO {

    private String fieldRefId;

    private String readEffect;

    private String writeEffect;
}
