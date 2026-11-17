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
