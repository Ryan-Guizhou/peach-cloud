package com.peach.auth.dao;

import com.peach.auth.qo.RoleQO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.vo.RoleVO;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:44
 */
@Indexed
@MybatisDao
public interface RoleDao extends PeachDao<RoleDO, RoleVO> {

    List<RoleVO> selectByUser(RoleQO roleQO);
}
