package com.peach.auth.dao;

import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

/**
 * 授权变更审计只追加写入，必须与业务授权及版本失效同事务提交。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
@Indexed
@MybatisDao
public interface IamAuthorizationAuditDao {

    /**
     * 在与授权变更相同的事务中插入完整操作信息。
     *
     * @param id 审计物理 ID
     * @param tenantRefId 租户物理 ID
     * @param appRefId 应用物理 ID
     * @param year 自然年度，成员级变更无具体年度时可为 null
     * @param operatorRefId 操作人成员物理 ID
     * @param targetRefId 授权绑定物理 ID
     * @param operation 授予或撤销操作
     * @param oldVersion 目标成员旧授权版本
     * @param newVersion 目标成员新授权版本
     * @return 插入条数
     */
    int append(@Param("id") String id,
               @Param("tenantRefId") String tenantRefId,
               @Param("appRefId") String appRefId,
               @Param("year") Integer year,
               @Param("operatorRefId") String operatorRefId,
               @Param("targetRefId") String targetRefId,
               @Param("operation") String operation,
               @Param("oldVersion") long oldVersion,
               @Param("newVersion") long newVersion);
}
