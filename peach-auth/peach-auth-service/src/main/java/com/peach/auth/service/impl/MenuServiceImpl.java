package com.peach.auth.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.MenuDTO;
import com.peach.auth.dao.MenuDao;
import com.peach.auth.entity.MenuDO;
import com.peach.auth.qo.MenuQO;
import com.peach.auth.service.IMenuService;
import com.peach.auth.vo.MenuVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;

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

    @Override
    public PageInfo<MenuVO> pageList(MenuQO menuQO) {
        PageInfo<MenuVO> pageInfo = PageHelper.startPage(menuQO.getPageNum(), menuQO.getPageSize())
                .doSelectPageInfo(() -> menuDao.selectByQO(menuQO));
        return pageInfo;
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
        Optional.ofNullable(menuDO.getIsDelete()).ifPresent(value -> menuDO.setIsDelete(value));
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
