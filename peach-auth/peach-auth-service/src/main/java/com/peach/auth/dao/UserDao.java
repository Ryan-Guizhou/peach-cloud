package com.peach.auth.dao;

import com.peach.auth.vo.UserVO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.UserQO;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.stereotype.Indexed;


import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 15:52
 */
@Indexed
@MybatisDao
public interface UserDao extends PeachDao<UserDO, UserVO> {

    UserVO login(@Param("username") String username,@Param("password") String password);
}
