package com.peach.auth.dao;

import com.peach.auth.entity.RoleDO;
import com.peach.auth.vo.RolePermissionGrantVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 平台级租户开通的受控持久化接口。
 *
 * <p>平台身份只能由真实系统租户和不可委派的 SYSTEM 权限证明；
 * 目标租户、应用、根机构及所有者角色在同一业务事务内创建。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 18:30
 */
@Indexed
@MybatisDao
public interface TenantProvisioningDao {

    /**
     * 锁定真实平台控制租户，串行化当前租户的管理员授权变更。
     *
     * @param tenantRefId 当前可信 Token 中的平台租户物理 ID
     * @return 物理 ID，非平台租户则返回 null
     */
    String lockPlatformTenant(@Param("tenantRefId") String tenantRefId);

    /**
     * 仅系统保护的非委派权限及 SYSTEM 角色可授权平台开通。
     */
    int countPlatformCreateGrant(@Param("tenantRefId") String tenantRefId,
                                 @Param("memberRefId") String memberRefId,
                                 @Param("appRefId") String appRefId);

    int insertTenant(@Param("id") String id, @Param("tenantId") String tenantId,
                     @Param("tenantCode") String tenantCode, @Param("tenantName") String tenantName,
                     @Param("operatorRefId") String operatorRefId, @Param("createdTime") String createdTime);

    int insertApplication(@Param("id") String id, @Param("appId") String appId,
                          @Param("tenantId") String tenantId, @Param("appName") String appName,
                          @Param("operatorRefId") String operatorRefId, @Param("createdTime") String createdTime);

    int insertRootOrganization(@Param("id") String id, @Param("orgId") String orgId,
                               @Param("tenantId") String tenantId, @Param("orgName") String orgName,
                               @Param("operatorRefId") String operatorRefId, @Param("createdTime") String createdTime);

    /**
     * 写入禁止普通租户管理员修改的 SYSTEM 所有者角色。
     */
    int insertOwnerRole(RoleDO role);

    /**
     * 仅查询系统定义、全局、有效且允许租户管理者委派的初始权限。
     */
    List<RolePermissionGrantVO> selectOwnerPermissions();
}
