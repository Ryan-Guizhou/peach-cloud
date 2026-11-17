package com.peach.auth.service.impl;

import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.MenuDTO;
import com.peach.auth.dao.MenuDao;
import com.peach.auth.entity.MenuDO;
import com.peach.auth.qo.MenuQO;
import com.peach.auth.service.IMenuService;
import com.peach.auth.vo.MenuVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.unique.UniqueIdFacade;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;


/**
 * 菜单服务实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:30
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements IMenuService {

    private final AuthBusinessIdGenerator authBusinessIdGenerator;

    private final MenuDao menuDao;

    @Override
    public PageInfo<MenuVO> pageList(MenuQO menuQO) {
        return PageMethod.startPage(menuQO.getPageNum(), menuQO.getPageSize())
                .doSelectPageInfo(() -> menuDao.selectByQO(menuQO));
    }

    @Override
    public MenuVO selectById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return new MenuVO();
        }
        return menuDao.selectById(id);
    }

    @Override
    public void add(MenuDTO menuDTO) {
        MenuDO menuDO = new MenuDO();
        BeanUtils.copyProperties(menuDTO, menuDO);
        menuDO.fillCreateTime(null);
        menuDO.setId(UniqueIdFacade.nextId());
        menuDO.setMenuId(authBusinessIdGenerator.next(AuthBusinessIdType.MENU));
        if (StringUtil.isBlank(menuDO.getTenantId()) || StringUtil.isBlank(menuDO.getAppId())) {
            throw new IllegalArgumentException("Tenant and application context are required");
        }
        if (menuDO.getIsDelete() == null) {
            menuDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        }
        menuDao.insert(menuDO);
    }

    @Override
    public void delById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return;
        }
        menuDao.delById(id);
    }

    @Override
    public void update(MenuDTO menuDTO) {
        MenuDO menuDO = new MenuDO();
        BeanUtils.copyProperties(menuDTO, menuDO);
        menuDO.fillModifyTime(null);
        menuDao.updateById(menuDO);
    }
}
