package com.peach.auth.dao;

import com.peach.auth.entity.UserOrgDO;
import com.peach.auth.vo.UserOrgVO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 用户机构关系 DAO。
 *
 * <p>用于维护用户与租户、机构之间的绑定关系，以及登录时的当前机构上下文查询。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Indexed
@MybatisDao
public interface UserOrgDao extends PeachDao<UserOrgDO, UserOrgVO> {

    List<UserOrgVO> selectByUserId(@Param("userId") String userId);

    List<UserOrgVO> selectByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);
}
