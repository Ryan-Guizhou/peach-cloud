package com.peach.userservice.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.userservice.entity.RoleDO;
import com.peach.userservice.vo.RoleVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:44
 */
@Indexed
@MybatisDao
public interface RoleDao extends PeachDao<RoleDO, RoleVO> {
}
