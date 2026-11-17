package com.peach.auth.dao;

import com.peach.auth.entity.RoleDO;
import com.peach.auth.vo.RolePermissionGrantVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 租户年度角色与权限的受控持久层。
 *
 * <p>角色行锁串行化所有角色权限写入；SQL 中所有关联都使用物理主键，
 * 不采用 ROLE_CODE 和机构业务号拼接权限事实。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Indexed
@MybatisDao
public interface ScopedRoleManagementDao {

    /**
     * 创建与单个租户、应用和自然年度绑定的租户级角色。
     *
     * @param role 已经服务端校验的角色
     * @return 插入条数
     */
    int insertRole(RoleDO role);

    /**
     * 锁定当前范围的角色，返回业务模型；不存在或无权访问则返回 null。
     *
     * @param tenantRefId 租户物理主键
     * @param appRefId 应用物理主键
     * @param year 自然年度
     * @param roleRefId 角色物理主键
     * @return 角色实体或 null
     */
    RoleDO lockScopedRole(@Param("tenantRefId") String tenantRefId,
                          @Param("appRefId") String appRefId,
                          @Param("year") int year,
                          @Param("roleRefId") String roleRefId);

    /**
     * 读取某一租户角色的当前有效权限物理主键。
     */
    List<String> selectActivePermissionRefs(@Param("tenantRefId") String tenantRefId,
                                            @Param("roleRefId") String roleRefId);

    /**
     * 将权限物理 ID 与稳定语义码关联，且只返回允许租户管理员委派的定义。
     */
    List<RolePermissionGrantVO> selectDelegablePermissions(
            @Param("tenantRefId") String tenantRefId,
            @Param("appRefId") String appRefId,
            @Param("permissionRefIds") List<String> permissionRefIds);

    /**
     * 新增当前角色的一条有效权限关系。
     */
    int insertRolePermission(@Param("id") String id,
                             @Param("tenantRefId") String tenantRefId,
                             @Param("roleRefId") String roleRefId,
                             @Param("permissionRefId") String permissionRefId,
                             @Param("operatorRefId") String operatorRefId);

    /**
     * 逻辑删除单条角色权限关系，保留原历史记录。
     */
    int revokeRolePermission(@Param("tenantRefId") String tenantRefId,
                             @Param("roleRefId") String roleRefId,
                             @Param("permissionRefId") String permissionRefId);

    /**
     * 变更角色自身版本，必须由事务中先前锁定的 ROLE.VERSION 确认。
     */
    int incrementRoleVersion(@Param("tenantRefId") String tenantRefId,
                             @Param("appRefId") String appRefId,
                             @Param("year") int year,
                             @Param("roleRefId") String roleRefId,
                             @Param("version") long version,
                             @Param("operatorRefId") String operatorRefId);
}
