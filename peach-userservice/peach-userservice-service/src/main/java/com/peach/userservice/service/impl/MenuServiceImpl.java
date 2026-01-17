package com.peach.userservice.service.impl;

import com.peach.userservice.dao.FunctionDao;
import com.peach.userservice.dao.MenuDao;
import com.peach.userservice.service.IFunctionService;
import com.peach.userservice.service.IMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:30
 */
@Slf4j
@Indexed
@Service
public class MenuServiceImpl implements IMenuService {

    @Resource
    private MenuDao menuDao;
}
