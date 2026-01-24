package com.peach.userservice.service;


import com.peach.common.response.Response;
import com.peach.userservice.dto.LoginDTO;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
public interface IUserService {


    Response login(LoginDTO loginDTO);

}
