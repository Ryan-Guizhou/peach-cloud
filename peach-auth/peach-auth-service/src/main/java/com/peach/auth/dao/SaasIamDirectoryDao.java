package com.peach.auth.dao;

import com.peach.auth.vo.SaasIamMemberBriefVO;
import com.peach.auth.vo.SaasIamOrganizationBriefVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 只读取当前物理租户内有效成员及机构的白名单列。
 *
 * <p>所有查询均以服务端已验证的 tenantRefId 为绑定参数，
 * 不根据请求提交的机构 ID 或角色业务码推断租户。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:50
 */
@Indexed
@MybatisDao
public interface SaasIamDirectoryDao {

    /**
     * 按租户物理 ID 读取有效成员，不输出密码、联系方式等敏感字段。
     */
    List<SaasIamMemberBriefVO> selectActiveMembers(@Param("tenantRefId") String tenantRefId,
                                                    @Param("ids") List<String> ids);

    /**
     * 联结租户物理主键与永久业务租户号后查询未删除有效机构。
     */
    List<SaasIamOrganizationBriefVO> selectActiveOrganizations(@Param("tenantRefId") String tenantRefId,
                                                                @Param("ids") List<String> ids);
}
