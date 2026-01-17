
package com.peach.userservice.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.userservice.dao.MenuDao;
import com.peach.userservice.dao.RoleDao;
import com.peach.userservice.entity.RoleDO;
import com.peach.userservice.entity.RouterDO;
import com.peach.userservice.qo.RoleQO;
import com.peach.userservice.service.IMenuService;
import com.peach.userservice.service.IRoleService;
import com.peach.userservice.vo.RoleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:30
 */
@Slf4j
@Indexed
@Service
public class RoleServiceImpl implements IRoleService {

    @Resource
    private RoleDao roleDao;

    @Override
    public PageInfo<RoleVO> pageList(RoleQO roleQO) {
        PageInfo<RoleVO> pageInfo = PageHelper.startPage(roleQO.getPageNum(), roleQO.getPageSize())
                .doSelectPageInfo(() -> roleDao.select(new RoleDO()));
        return pageInfo;
    }

    @Override
    public List<RoleVO> list(RoleQO roleQO) {
        return Collections.emptyList();
    }

    @Override
    public RoleVO selectById(String id) {
        return roleDao.selectById(id);
    }

    @Override
    public void add(RoleQO roleQO) {

    }

    @Override
    public void delById(String id) {

    }

    @Override
    public void update(RoleQO roleQO) {

    }
}
