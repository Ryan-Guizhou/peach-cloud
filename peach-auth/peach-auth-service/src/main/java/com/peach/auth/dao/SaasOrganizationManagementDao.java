package com.peach.auth.dao;

import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.vo.OrganizationVO;

import java.util.List;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

/**
 * SaaS 机构的物理租户限定写入及删除前依赖检查。
 *
 * <p>只能由持有租户写锁和有效 TENANT.ORG.MANAGE 的领域服务调用。
 * 修改采用机构版本 CAS；删除前检查所有受保护的关联数据。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:00
 */
@Indexed
@MybatisDao
public interface SaasOrganizationManagementDao {

    int insert(OrganizationDO organization);

    List<OrganizationVO> selectActiveInTenant(@Param("tenantBusinessId") String tenantBusinessId);

    OrganizationVO selectByIdInTenant(@Param("id") String id,
                                      @Param("tenantBusinessId") String tenantBusinessId);

    int updateName(@Param("id") String id,
                   @Param("tenantBusinessId") String tenantBusinessId,
                   @Param("orgName") String orgName,
                   @Param("version") long version,
                   @Param("operatorRefId") String operatorRefId);

    int countDependencies(@Param("tenantRefId") String tenantRefId,
                          @Param("tenantBusinessId") String tenantBusinessId,
                          @Param("orgRefId") String orgRefId,
                          @Param("orgId") String orgId);

    int retire(@Param("id") String id,
               @Param("tenantBusinessId") String tenantBusinessId,
               @Param("version") long version,
               @Param("operatorRefId") String operatorRefId);
}
