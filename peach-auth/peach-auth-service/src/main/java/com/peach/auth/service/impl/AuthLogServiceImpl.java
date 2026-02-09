package com.peach.auth.service.impl;

import com.peach.auth.dao.AuthLogDao;
import com.peach.auth.service.IAuthLogService;
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
public class AuthLogServiceImpl implements IAuthLogService {

    @Resource
    private AuthLogDao authLogDao;

}
