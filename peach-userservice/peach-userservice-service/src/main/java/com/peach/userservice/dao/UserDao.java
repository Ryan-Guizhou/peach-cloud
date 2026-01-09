package com.peach.userservice.dao;

import com.peach.common.annoation.MybatisDao;
import com.peach.userservice.entity.UserDO;
import com.peach.userservice.qo.UserQO;


import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 15:52
 */
@MybatisDao
public interface UserDao {

    List<UserDO> select(UserDO userDO);

    UserDO selectById(String id);

    List<UserDO> selectByQO(UserQO userQO);
}
