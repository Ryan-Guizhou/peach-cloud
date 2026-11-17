package com.peach.auth.dao;

import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

/**
 * 授权变更时的成员版本失效接口，仅供完成权限校验的事务服务调用。
 *
 * <p>对当前租户的成员版本只增不减；不能依赖 Token TTL 代替权限撤销。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
@Indexed
@MybatisDao
public interface MemberAuthVersionDao {

    /**
     * 失效特定成员的所有旧授权快照。
     *
     * @param tenantRefId 租户物理 ID
     * @param memberRefId 成员物理 ID
     * @return 修改行数
     */
    int incrementForMember(@Param("tenantRefId") String tenantRefId,
                           @Param("memberRefId") String memberRefId);

    /**
     * 角色权限变更时失效租户成员的全部旧授权快照。
     *
     * @param tenantRefId 租户物理 ID
     * @return 修改行数
     */
    int incrementForTenant(@Param("tenantRefId") String tenantRefId);
}
