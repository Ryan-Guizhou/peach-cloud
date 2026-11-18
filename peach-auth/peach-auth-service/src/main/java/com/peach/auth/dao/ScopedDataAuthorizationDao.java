package com.peach.auth.dao;

import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.ScopedFieldRuleVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 从真实租户成员、角色绑定和资源定义读出同一动作的行与字段授权。
 *
 * <p>全部 JOIN 使用物理主键和服务端身份，不接受客户端拼接 SQL 片段。
 * 查询缺少政策或服务端资源目录时默认没有授权证据。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
@Indexed
@MybatisDao
public interface ScopedDataAuthorizationDao {

    /**
     * 获得当前成员/应用/中某一动作的独立绑定与数据策略证据。
     *
     * @param tenantRefId 物理租户主键
     * @param memberRefId 物理成员主键
     * @param appRefId 物理应用主键
     * @param permissionCode 业务动作的稳定语义码
     * @return 该权限的行策略证据
     */
    List<RoleDataGrantVO> selectDataGrants(@Param("tenantRefId") String tenantRefId,
                                           @Param("memberRefId" String memberRefId,
                                           @Param("appRefId") String appRefId,
                                           @Param("permissionCode" String permissionCode);

    /**
     * 只查询已匹配行级授权的角色绑定对应的字段策略。
     *
     * @param tenantRefId 物理租户主键
     * @param memberRefId 物理成员主键
     * @param appRefId 物理应用主键
     * @param permissionCode 业务动作权限码
     * @param resourceCode 后端受控稳定资源类型
     * @param bindingRefIds 已经行级授权的物理绑定 ID
     * @return 对应绑定内定义的允许/拒绝/脱敏字段
     */
    List<ScopedFieldRuleVO> selectFieldRules(@Param("tenantRefId") String tenantRefId,
                                             @Param("memberRefId" String memberRefId,
                                             @Param("appRefId") String appRefId,
                                             @Param("permissionCode" String permissionCode,
                                             @Param("resourceCode") String resourceCode,
                                             @Param("bindingRefIds") List<String> bindingRefIds);
}
