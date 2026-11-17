package com.peach.auth.dao;

import com.peach.auth.vo.RoleScopeGrantVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 从角色权限和成员角色绑定关联查询有效的单条授权证据。
 *
 * <p>所有过滤条件都使用数据库物理 ID、自然年度以及同一条绑定，禁止拼接不同绑定的权限。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
@Indexed
@MybatisDao
public interface ScopedRoleGrantDao {

    /**
     * 查询指定动作在当前成员下的全部合法单条授权证据。
     *
     * @param tenantRefId 租户物理 ID
     * @param memberRefId 成员物理 ID
     * @param appRefId 应用物理 ID
     * @param year 自然年度
     * @param permissionCode 稳定权限语义码
     * @return 每行对应独立角色绑定，空列表即无权限
     */
    List<RoleScopeGrantVO> selectGrants(@Param("tenantRefId") String tenantRefId,
                                       @Param("memberRefId") String memberRefId,
                                       @Param("appRefId") String appRefId,
                                       @Param("year") int year,
                                       @Param("permissionCode") String permissionCode);
}
