package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.ApplicationDO;
import com.peach.auth.vo.ApplicationVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:43
 */
@Indexed
@MybatisDao
public interface ApplicationDao extends PeachDao<ApplicationDO, ApplicationVO> {
}
