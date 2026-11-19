package com.peach.fileservice.dao;

import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

/**
 * 从物理租户验证合法启用的永久业务租户编号。
 *
 * <p>不得使用文件上传 DTO 或历史 USER_PROFILE 中自报的 TENANT_ID。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Indexed
@MybatisDao
public interface FileSaasScopeDao {

    /**
     * 校验物理租户启用，取得后端可信永久业务租户编号。
     *
     * @param tenantRefId IAM 已核验的租户 UUID
     * @return 业务编号；不存在返回 null
     */
    String findEnabledTenantBusinessId(@Param("tenantRefId") String tenantRefId);
}
