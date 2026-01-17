package com.peach.userservice.service.impl;

import com.peach.userservice.dao.AuthPartyDao;
import com.peach.userservice.dao.AuthResourceDao;
import com.peach.userservice.service.IAuthPartyService;
import com.peach.userservice.service.IAuthResourceService;
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
public class AuthResourceServiceImpl implements IAuthResourceService {

    @Resource
    private AuthResourceDao authResourceDao;

}
