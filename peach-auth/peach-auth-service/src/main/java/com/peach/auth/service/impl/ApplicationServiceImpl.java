package com.peach.auth.service.impl;

import lombok.RequiredArgsConstructor;

import com.peach.auth.dao.ApplicationDao;
import com.peach.auth.service.IApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 应用服务实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:27
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements IApplicationService {

    private final ApplicationDao applicationDao;

}
