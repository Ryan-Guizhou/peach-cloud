package com.peach.auth.dao;

import com.peach.auth.entity.TenantInvitationDO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

/**
 * 租户成员邀请持久化接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 15:30
 */
@Indexed
@MybatisDao
public interface TenantInvitationDao {

    int insert(TenantInvitationDO invitation);

    /**
     * 清理目标用户已过期的待接受邀请，为重新邀请腾出唯一活动槽位。
     *
     * @param tenantRefId 租户物理主键
     * @param userRefId 受邀用户物理主键
     * @return 变更记录数
     */
    int expirePendingForUser(@Param("tenantRefId") String tenantRefId,
                             @Param("userRefId") String userRefId);

    /**
     * 按租户范围查询邀请，用于幂等确认和状态校验。
     *
     * @param id 邀请物理主键
     * @param tenantRefId 租户物理主键
     * @return 邀请记录或 null
     */
    TenantInvitationDO selectByIdInTenant(@Param("id") String id,
                                          @Param("tenantRefId") String tenantRefId);

    /**
     * 按受信账号反查其待接受邀请的目标租户，禁止自报 tenantRefId。
     */
    String findTenantForVerifiedInvitee(@Param("id") String id,
                                        @Param("userRefId") String userRefId);

    TenantInvitationDO selectPending(@Param("id") String id,
                                      @Param("tenantRefId") String tenantRefId);

    int accept(@Param("id") String id,
               @Param("tenantRefId") String tenantRefId,
               @Param("userRefId") String userRefId,
               @Param("version") long version);

    int cancel(@Param("id") String id,
               @Param("tenantRefId") String tenantRefId,
               @Param("version") long version,
               @Param("operatorRefId") String operatorRefId);
}
