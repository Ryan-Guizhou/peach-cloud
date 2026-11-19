package com.peach.auth.dao;

import com.peach.auth.vo.SaasCodeRuleVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * SaaS 编码规则的只读目录与平台不可委派权限事实查询。
 *
 * <p>规则查询必须同时限定服务端确定的命名空间和作用域；
 * 平台管理动作必须由同一有效 SYSTEM 角色、当前成员与应用证明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:15
 */
@Indexed
@MybatisDao
public interface SaasCodeRuleDao {

    /**
     * 确认 Token 所属物理租户确为启用状态的 PLATFORM 控制租户。
     *
     * @param tenantRefId 可信 Token 物理租户
     * @return 真正的平台物理租户 ID，不存在返回 null
     */
    String findPlatformControlTenant(@Param("tenantRefId") String tenantRefId);

    /**
     * 验证当前有效 SYSTEM 角色确实持有不可委派的平台编码管理权限。
     */
    int countProtectedPlatformManagerGrant(@Param("tenantRefId") String tenantRefId,
                                            @Param("memberRefId") String memberRefId,
                                            @Param("appRefId") String appRefId,
                                            @Param("memberVersion") long memberVersion,
                                            @Param("authVersion") long authVersion);

    /**
     * 有界查询当前命名空间的规则，额外一条用于检测下一页。
     */
    List<SaasCodeRuleVO> selectRules(@Param("namespace") String namespace,
                                     @Param("scopeType") String scopeType,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    /**
     * 查询现有且尚未逻辑删除的规则，不接受任意 SQL 或跨租户标识。
     */
    SaasCodeRuleVO selectRule(@Param("namespace") String namespace,
                              @Param("scopeType") String scopeType,
                              @Param("prefix") String prefix);
}
