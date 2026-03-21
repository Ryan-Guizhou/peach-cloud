package com.peach.auth.service;


import com.peach.auth.dto.RegisterDTO;
import com.peach.common.response.Response;
import com.peach.auth.dto.LoginDTO;
import com.peach.auth.vo.UserVO;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
public interface IUserService {


    Response login(LoginDTO loginDTO);

    Response register(RegisterDTO registerDTO);

    UserVO selectUserById(String id);

}
