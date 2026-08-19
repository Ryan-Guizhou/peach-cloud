package com.peach.auth.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.common.validate.CommonValidator;
import com.peach.auth.dao.RouterDao;
import com.peach.auth.dto.RouterDTO;
import com.peach.auth.entity.RouterDO;
import com.peach.auth.qo.RouterQO;
import com.peach.auth.service.IRouterService;
import com.peach.auth.vo.RouterVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:27
 */
@Slf4j
@Indexed
@Service
public class RouterServiceImpl implements IRouterService {

    @Resource
    private RouterDao routerDao;

    @Resource
    private CommonValidator commonValidator;

    @PostConstruct
    public void init() {
        String id = null;
        commonValidator.validate(id);
    }

    @Override
    public void delById(String id) {
        boolean valid = commonValidator.isValid(id);
        if (!valid) {
            log.error("参数验证失败");
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
        PageInfo<RouterVO> pageInfo = PageHelper.startPage(routerQO.getPageNum(), routerQO.getPageSize())
                .doSelectPageInfo(() -> routerDao.selectByQO(routerQO));
        return pageInfo;
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
