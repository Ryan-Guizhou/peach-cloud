package com.peach.auth.dao;

import com.peach.auth.entity.MemberRoleBindingDO;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 成员角色授权的受控事务持久层。
 *
 * <p>所有读写同时限定租户物理 ID；成员行锁序列化同成员授权变更，
 * 角色与权限定义必须在当前租户和应用范围内保持完整关系。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
@Indexed
@MybatisDao
public interface ScopedRoleBindingDao {

    /**
     * 锁定待授权成员，必须已启用且其全局账号有效。
     *
     * @param tenantRefId 租户物理主键
     * @param memberRefId 成员物理主键
     * @return 成员或 null
     */
    TenantMemberDO lockActiveMember(@Param("tenantRefId") String tenantRefId,
                                    @Param("memberRefId") String memberRefId);

    /**
     * 验证角色的物理租户、应用归属与有效状态。
     *
     * @param tenantRefId 租户物理主键
     * @param appRefId 应用物理主键
     * @param roleRefId 角色物理主键
     * @return 合法且有效的角色数
     */
    int countActiveRole(@Param("tenantRefId") String tenantRefId,
                        @Param("appRefId") String appRefId,
                        @Param("roleRefId") String roleRefId);

    /**
     * 查询角色全部有效绑定权限的数量，包括不可委派权限。
     */
    int countActiveRolePermissions(@Param("tenantRefId") String tenantRefId,
                                   @Param("roleRefId") String roleRefId);

    /**
     * 查询当前租户与应用允许委派的角色权限语义码。
     *
     * <p>结果数量必须与角色的全部活动权限数量一致，才可允许授权。</p>
     */
    List<String> selectDelegablePermissionCodes(@Param("tenantRefId") String tenantRefId,
                                                @Param("appRefId") String appRefId,
                                                @Param("roleRefId") String roleRefId);

    /**
     * 查询完全一致且仍有效的独立授权绑定。
     */
    MemberRoleBindingDO selectActiveBinding(@Param("tenantRefId") String tenantRefId,
                                            @Param("memberRefId") String memberRefId,
                                            @Param("appRefId") String appRefId,
                                            @Param("roleRefId") String roleRefId,
                                            @Param("scopeType") String scopeType,
                                            @Param("scopeRefId") String scopeRefId);

    /**
     * 按 ID 查询租户内的有效授权绑定，用于事务撤销。
     */
    MemberRoleBindingDO selectActiveById(@Param("tenantRefId") String tenantRefId,
                                         @Param("id") String id);

    /**
     * 插入一条独立绑定；历史授权不会使用原 ID。
     */
    int insert(MemberRoleBindingDO binding);

    /**
     * 逻辑撤销单条授权，以版本及当前活动状态作为 CAS 条件。
     */
    int revoke(@Param("tenantRefId") String tenantRefId,
               @Param("id") String id,
               @Param("version") long version);

    /**
     * 当成员退出或被停用时永久逻辑撤销其全部活动角色绑定。
     *
     * <p>重复恢复成员身份不应复活已经失效的角色授权。</p>
     *
     * @param tenantRefId 租户物理主键
     * @param memberRefId 成员物理主键
     * @return 被逻辑撤销的有效授权条数
     */
    int revokeAllForMember(@Param("tenantRefId") String tenantRefId,
                           @Param("memberRefId") String memberRefId);
}
