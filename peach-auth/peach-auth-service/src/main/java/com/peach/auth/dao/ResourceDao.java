package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.userservice.entity.ResourceDO;
import com.peach.userservice.vo.ResourceVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:44
 */
@Indexed
@MybatisDao
public interface ResourceDao extends PeachDao<ResourceDO, ResourceVO> {
}
