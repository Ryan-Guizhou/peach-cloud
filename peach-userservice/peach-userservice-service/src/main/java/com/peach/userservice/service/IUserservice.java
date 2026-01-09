package com.peach.userservice.service;


import com.peach.userservice.qo.UserQO;
import com.peach.userservice.vo.UserVO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
public interface IUserservice {

    List<UserVO> list(UserQO qo);

    UserVO getById(String id);
}
