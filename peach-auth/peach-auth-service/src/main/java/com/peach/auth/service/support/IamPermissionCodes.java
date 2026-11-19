package com.peach.auth.service.support;

/**
 * 平台保护的固定权限语义码。
 *
 * <p>固定代码不由业务发号器生成，也不得由租户自定义权限覆盖或修改含义。
 * 仅服务端已经验证的授权关系可引用这些代码。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
public final class IamPermissionCodes {

    /** 租户成员邀请、启用和停用管理权限。 */
    public static final String TENANT_MEMBER_MANAGE = "TENANT.MEMBER.MANAGE";

    /** 租户机构树和机构资料管理权限。 */
    public static final String TENANT_ORG_MANAGE = "TENANT.ORG.MANAGE";

    /** 租户角色和成员角色绑定管理权限。 */
    public static final String TENANT_ROLE_MANAGE = "TENANT.ROLE.MANAGE";

    /** 当前租户的编码规则维护权限，仅对本租户命名空间生效。 */
    public static final String TENANT_CODE_RULE_MANAGE = "TENANT.CODE.RULE.MANAGE";

    /** PLATFORM 控制租户的不可委派全局发号规则管理权限。 */
    public static final String PLATFORM_CODE_RULE_MANAGE = "PLATFORM.CODE.RULE.MANAGE";

    private IamPermissionCodes() {
        throw new IllegalStateException("Utility class");
    }
}
