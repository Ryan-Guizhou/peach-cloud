package com.peach.auth.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.userservice.entity.UserDO;
import com.peach.userservice.qo.UserQO;
import org.springframework.stereotype.Indexed;


import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 15:52
 */
@Indexed
@MybatisDao
public interface UserDao extends PeachDao<UserDO, UserDO> {

}
