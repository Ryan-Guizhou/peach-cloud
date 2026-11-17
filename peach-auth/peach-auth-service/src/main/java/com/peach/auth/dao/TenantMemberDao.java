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
