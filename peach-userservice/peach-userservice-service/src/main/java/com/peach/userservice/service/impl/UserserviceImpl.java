package com.peach.userservice.service.impl;

import com.peach.userservice.dao.UserDao;
import com.peach.userservice.service.IUserservice;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
@Service
public class UserserviceImpl implements IUserservice {

    @Resource
    private UserDao userDao;

}
