package com.peach.auth.dao;

import com.peach.auth.vo.RolePermissionGrantVO;
import com.peach.auth.vo.SaasCatalogEntryVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 受保护角色动作以及可委派字段目录的只读物理身份查询。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:55
 */
@Indexed
@MybatisDao
public interface SaasPolicyCatalogDao {

    RolePermissionGrantVO selectScopedRoleAction(@Param("tenantRefId") String tenantRefId,
                                                 @Param("appRefId") String appRefId,
                                                 @Param("year") int year,
                                                 @Param("roleRefId") String roleRefId,
                                                 @Param("permissionRefId") String permissionRefId);

    List<SaasCatalogEntryVO> selectDelegableFields(@Param("tenantRefId") String tenantRefId,
                                                    @Param("appRefId") String appRefId,
                                                    @Param("resourceCode") String resourceCode,
                                                    @Param("limit") int limit,
                                                    @Param("offset") int offset);
}
