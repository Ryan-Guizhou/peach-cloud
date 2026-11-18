package com.peach.auth.dao;

import com.peach.auth.vo.RoleScopeGrantVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 从角色权限和成员角色绑定关联查询有效的单条授权证据。
 *
 * <p>所有过滤条件都使用数据库物理 ID、以及同一条绑定，禁止拼接不同绑定的权限。</p>
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
     * @param permissionCode 稳定权限语义码
     * @return 每行对应独立角色绑定，空列表即无权限
     */
    List<RoleScopeGrantVO> selectGrants(@Param("tenantRefId") String tenantRefId,
                                       @Param("memberRefId") String memberRefId,
                                       @Param("appRefId") String appRefId,
                                       @Param("permissionCode") String permissionCode);
    /**
     * 读取所有当前租户/应用可见的有效 API 目录规则，包括尚未分配给当前成员的定义。
     *
     * <p>为避免较宽泛的模板掩盖尚未授权的精确路径，必须先在完整目录解析，
     * 再查询当前成员授权。查询最多读取 513 条，超过 512 则拒绝。</p>
     *
     * @param tenantRefId 当前已验证租户的物理 ID
     * @param appRefId 当前已验证应用的物理 ID
     * @param method 已验证的 HTTP 方法
     * @return 有界权限码；超额拒绝，不截断使用
     */
    List<String> selectVisibleApiCodes(@Param("tenantRefId") String tenantRefId,
                                       @Param("appRefId") String appRefId,
                                       @Param("method") String method);

    /**
     * 查询 API 类资源的当前动作授权，严格拒绝 DATA/MENU/FILE 等不同资源类型借用。
     *
     * @param tenantRefId 已核验的租户物理主键
     * @param memberRefId 已核验的成员物理主键
     * @param appRefId 已核验的应用物理主键
     * @param permissionCode 服务端归一化的精确 METHOD:PATH
     * @param systemOnly 平台服务命名空间必须仅接受 SYSTEM 权限定义
     * @return 有效且属于同一成员绑定的 API 权限事实
     */
    List<RoleScopeGrantVO> selectApiGrants(@Param("tenantRefId") String tenantRefId,
                                          @Param("memberRefId") String memberRefId,
                                          @Param("appRefId") String appRefId,
                                          @Param("permissionCode") String permissionCode,
                                          @Param("systemOnly") boolean systemOnly);
}
