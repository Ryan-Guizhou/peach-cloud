package com.peach.auth.dao;

import com.peach.auth.entity.OrganizationDO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

/**
 * 组织树物理引用与租户序列化移动的专用 DAO。
 *
 * <p>租户记录 FOR UPDATE 作为同租户组织树写锁，避免交叉移动并发产生环。
 * 查询机构必须同时限定服务端确定的租户归属和未删除状态。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:10
 */
@Indexed
@MybatisDao
public interface OrganizationHierarchyDao {

    /**
     * 锁定启用租户，返回用于过渡期校验现有机构数据的永久业务租户 ID。
     *
     * @param tenantRefId 租户物理主键
     * @return 租户业务 ID，不存在则返回 null
     */
    String lockTenantAndFindBusinessId(@Param("tenantRefId") String tenantRefId);

    /**
     * 只读查询当前已启用租户的业务 ID，不获取写锁。
     *
     * @param tenantRefId 服务端可信租户物理 ID
     * @return 已启用租户业务编号或 null
     */
    String findEnabledTenantBusinessId(@Param("tenantRefId") String tenantRefId);

    /**
     * 查询某租户下未删除的机构及其父节点物理 ID。
     *
     * @param id 机构物理 ID
     * @param tenantBusinessId 由锁定的租户行获得的业务租户 ID
     * @return 机构或 null
     */
    OrganizationDO selectInTenant(@Param("id") String id,
                                  @Param("tenantBusinessId") String tenantBusinessId);

    /**
     * 通过乐观版本控制修改父机构，要求操作者在调用前已完成管理授权。
     *
     * @param id 机构物理 ID
     * @param tenantBusinessId 服务端租户业务 ID
     * @param newParentRefId 新父机构物理 ID，null 表示根机构
     * @param version 当前版本
     * @param modifierRefId 操作者成员物理 ID
     * @return 成功修改行数
     */
    int updateParent(@Param("id") String id,
                     @Param("tenantBusinessId") String tenantBusinessId,
                     @Param("newParentRefId") String newParentRefId,
                     @Param("version") long version,
                     @Param("modifierRefId") String modifierRefId);
}
