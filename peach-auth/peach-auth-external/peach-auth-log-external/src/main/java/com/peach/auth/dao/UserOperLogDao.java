package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.UserOperLogDO;
import com.peach.auth.vo.UserOperLogVO;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 18:51
 */
@Indexed
@MybatisDao
public interface UserOperLogDao extends PeachDao<UserOperLogDO, UserOperLogVO> {

}
