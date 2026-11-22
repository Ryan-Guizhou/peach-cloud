package com.peach.auth.dao;
import com.peach.auth.entity.TenantDO;
import com.peach.auth.vo.TenantVO;
import com.peach.auth.qo.TenantQO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import java.util.List;
/**
 * 租户管理。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 18:00
 */
@MybatisDao
public interface TenantDao {
    List<TenantVO> selectByQO(TenantQO query);
    TenantVO selectById(@Param("id") String id);
    TenantVO selectByTenantId(@Param("tenantId") String tenantId);
    int countByCode(@Param("code") String code, @Param("excludeId") String excludeId);
    int insert(TenantDO tenant);
    int updateDetails(TenantDO tenant);
    int updateStatus(TenantDO tenant);
    int logicalDelete(TenantDO tenant);
}
