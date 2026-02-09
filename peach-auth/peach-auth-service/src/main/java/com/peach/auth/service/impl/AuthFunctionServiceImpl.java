package com.peach.auth.service.impl;

import com.peach.auth.dao.AuthFunctionDao;
import com.peach.auth.service.IAuthFunctionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:27
 */
@Slf4j
@Indexed
@Service
public class AuthFunctionServiceImpl implements IAuthFunctionService {

    @Resource
    private AuthFunctionDao authFunctionDao;

}
