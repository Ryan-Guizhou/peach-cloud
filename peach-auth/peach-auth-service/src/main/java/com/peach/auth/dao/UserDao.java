package com.peach.auth.dao;

import com.peach.auth.vo.UserVO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.UserQO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;


import java.util.List;

/**
 * 用户数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 15:52
 */
@Indexed
@MybatisDao
public interface UserDao extends PeachDao<UserDO, UserVO> {

    UserVO login(@Param("username") String username, @Param("password") String password);

    List<UserVO> selectByQO(UserQO userQO);

    int updateProfileBasic(@Param("userId") String userId, @Param("userName") String userName,
                           @Param("mobilePhone") String mobilePhone, @Param("email") String email,
                           @Param("modifyTime") String modifyTime, @Param("modifierId") String modifierId);
}
