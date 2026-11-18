package com.peach.auth.dao;

import com.peach.auth.vo.SaasLoginGrantRowVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 从当前有效成员、角色及权限定义中查询受信的登录权限事实。
 *
 * <p>查询在 SQL 层对物理 ID、、成员版本与授权版本做完全限定，
 * 最多读取 1025 行，调用方对超过 1024 行的结果必须安全拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:10
 */
@Indexed
@MybatisDao
public interface SaasLoginSnapshotDao {

    /**
     * 查询有界的 SaaS 登录角色及有效资源明细。
     *
     * @param userRefId 登录用户物理主键
     * @param tenantRefId 当前租户物理主键
     * @param memberRefId 当前成员物理主键
     * @param appRefId 当前应用物理主键
     * @param memberVersion 成员版本
     * @param authVersion 当前角色授权版本
     * @return 单绑定授权事实，超过 1024 行应视为不完整快照并拒绝
     */
    List<SaasLoginGrantRowVO> selectCurrentGrants(@Param("userRefId") String userRefId,
                                                  @Param("tenantRefId") String tenantRefId,
                                                  @Param("memberRefId" String memberRefId,
                                                  @Param("appRefId") String appRefId,
                                                  @Param("memberVersion" long memberVersion,
                                                  @Param("authVersion") long authVersion);
}
