package com.peach.auth.service.impl;

import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dao.RouterDao;
import com.peach.auth.dto.RouterDTO;
import com.peach.auth.entity.RouterDO;
import com.peach.auth.qo.RouterQO;
import com.peach.auth.service.IRouterService;
import com.peach.auth.service.support.CommonValidator;
import com.peach.auth.vo.RouterVO;
import com.peach.common.unique.UniqueIdFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路由服务实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:27
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class RouterServiceImpl implements IRouterService {

    private final AuthBusinessIdGenerator authBusinessIdGenerator;

    private final RouterDao routerDao;

    private final CommonValidator commonValidator;

@Override
    public void delById(String id) {
        boolean valid = commonValidator.isValid(id);
        if (!valid) {
            log.error("Parameter validation failed");
            return;
        }
        routerDao.delById(id);
    }

    @Override
    public void update(RouterDTO routerDTO) {
        commonValidator.validateProperty(routerDTO,"routerId");
        RouterDO routerDO = new RouterDO();
        BeanUtils.copyProperties(routerDTO,routerDO);
        routerDao.update(routerDO);
    }

    @Override
    public void add(RouterDTO routerDTO) {
        RouterDO routerDO = new RouterDO();
        BeanUtils.copyProperties(routerDTO,routerDO);
        routerDO.setId(UniqueIdFacade.nextId());
        routerDO.setRouterId(authBusinessIdGenerator.next(AuthBusinessIdType.ROUTER));
        routerDao.insert(routerDO);
    }

    @Override
    public RouterVO selectById(String id) {
        commonValidator.validate(id);
        return routerDao.selectById(id);
    }

    @Override
    public List<RouterVO> list(RouterQO routerQO) {
        return routerDao.selectByQO(routerQO);
    }

    @Override
    public PageInfo<RouterVO> pageList(RouterQO routerQO) {
        return PageMethod.startPage(routerQO.getPageNum(), routerQO.getPageSize())
                .doSelectPageInfo(() -> routerDao.selectByQO(routerQO));
    }

    @Override
    public boolean uniqueRouterCode(RouterDTO routerDTO) {
        RouterDO routerDO = new RouterDO();
        routerDO.setRouterCode(routerDO.getRouterCode());
        routerDO.setRouterId(routerDTO.getRouterId());
        int count = routerDao.countByRouterCode(routerDO);
        return count > 0;
    }
}
