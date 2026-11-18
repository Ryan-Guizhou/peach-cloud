package com.peach.auth.dao;

import com.peach.auth.vo.SaasCatalogEntryVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * SaaS 当前租户/应用/的角色、成员、机构及可委派权限只读目录。
 *
 * <p>每一项查询都在 SQL WHERE 中强制限定物理租户与当前应用；
 * 仅受信领域服务可以调用，控制器不接受请求者自报租户/成员 ID。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:30
 */
@Indexed
@MybatisDao
public interface SaasRoleCatalogDao {

    List<SaasCatalogEntryVO> selectRoles(@Param("tenantRefId") String tenantRefId,
                                         @Param("appRefId") String appRefId,
                                         @Param("keyword") String keyword,
                                         @Param("limit") int limit,
                                         @Param("offset") int offset);

    List<SaasCatalogEntryVO> selectDelegablePermissions(@Param("tenantRefId") String tenantRefId,
                                                         @Param("appRefId") String appRefId,
                                                         @Param("memberRefId") String memberRefId,
                                                         @Param("keyword") String keyword,
                                                         @Param("limit") int limit,
                                                         @Param("offset") int offset);

    List<SaasCatalogEntryVO> selectMembers(@Param("tenantRefId") String tenantRefId,
                                           @Param("keyword") String keyword,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    List<SaasCatalogEntryVO> selectOrganizations(@Param("tenantRefId") String tenantRefId,
                                                 @Param("keyword") String keyword,
                                                 @Param("limit") int limit,
                                                 @Param("offset") int offset);

    List<String> selectRolePermissionRefs(@Param("tenantRefId") String tenantRefId,
                                          @Param("appRefId") String appRefId,
                                          @Param("roleRefId") String roleRefId);

    List<SaasCatalogEntryVO> selectMemberBindings(@Param("tenantRefId") String tenantRefId,
                                                   @Param("appRefId") String appRefId,
                                                   @Param("memberRefId") String memberRefId);
}
