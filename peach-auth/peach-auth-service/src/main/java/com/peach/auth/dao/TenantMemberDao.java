package com.peach.auth.dao;

import com.peach.auth.entity.TenantMemberDO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 租户成员持久化接口，全部按物理主键与有效状态操作。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 15:30
 */
@Indexed
@MybatisDao
public interface TenantMemberDao {

    int insert(TenantMemberDO member);

    TenantMemberDO selectByUserAndTenant(@Param("tenantRefId") String tenantRefId,
                                          @Param("userRefId") String userRefId);

    List<TenantMemberDO> selectActiveByUser(@Param("userRefId") String userRefId);

    /**
     * 根据成员物理主键及租户范围查询，供管理操作校验归属。
     *
     * @param id 成员物理主键
     * @param tenantRefId 租户物理主键
     * @return 成员记录或 null
     */
    TenantMemberDO selectByIdInTenant(@Param("id") String id,
                                      @Param("tenantRefId") String tenantRefId);

    /**
     * 服务侧校验指定租户处于启用状态。
     *
     * @param tenantRefId 租户物理主键
     * @return 启用且未删除的租户数量
     */
    int countEnabledTenant(@Param("tenantRefId") String tenantRefId);

    /**
     * 校验全局用户未删除且可用，禁止停用用户接受邀请。
     *
     * @param userRefId 用户物理主键
     * @return 有效用户数量
     */
    int countEnabledUser(@Param("userRefId") String userRefId);

    /**
     * 校验 Token 中的全局业务用户 ID 对应指定物理用户 ID。
     *
     * @param businessUserId 从 Sa-Token 服务端会话获取的永久业务用户 ID
     * @param userRefId 用户物理主键
     * @return 未停用且未删除的匹配用户数量
     */
    int countAuthenticatedUserIdentity(@Param("businessUserId") String businessUserId,
                                       @Param("userRefId") String userRefId);

    /**
     * 仅允许当前租户下已开放的应用参与 Token 会话。
     *
     * @param tenantRefId 租户物理主键
     * @param appRefId 应用物理主键
     * @return 有效应用数量
     */
    int countEnabledApplication(@Param("tenantRefId") String tenantRefId,
                                 @Param("appRefId") String appRefId);

    int changeStatus(@Param("id") String id,
                     @Param("tenantRefId") String tenantRefId,
                     @Param("status") String status,
                     @Param("version") long version,
                     @Param("modifierId") String modifierId,
                     @Param("modifyTime") String modifyTime);

    int softDelete(@Param("id") String id,
                   @Param("tenantRefId") String tenantRefId,
                   @Param("version") long version,
                   @Param("operatorRefId") String operatorRefId);

    int restore(@Param("id") String id,
                @Param("tenantRefId") String tenantRefId,
                @Param("version") long version);
}
