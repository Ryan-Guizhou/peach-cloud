package com.peach.auth.service.impl;

import com.peach.auth.dao.FunctionDao;
import com.peach.auth.service.IFunctionService;
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
public class FunctionServiceImpl implements IFunctionService {

    @Resource
    private FunctionDao functionDao;
}
