package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.AuthFunctionDO;
import com.peach.auth.vo.AuthFunctionVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.Collection;
import java.util.List;

/**
 * 认证功能数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:47
 */
@Indexed
@MybatisDao
public interface AuthFunctionDao extends PeachDao<AuthFunctionDO, AuthFunctionVO> {

    List<AuthFunctionVO> selectByPartyCodes(@Param("tenantId") String tenantId,
                                            @Param("orgId") String orgId,
                                            @Param("appId") String appId,
                                            @Param("fiscal") Integer fiscal,
                                            @Param("partyCodes") Collection<String> partyCodes);
}
