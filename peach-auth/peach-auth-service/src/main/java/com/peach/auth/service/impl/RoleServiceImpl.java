
package com.peach.auth.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.common.util.StringUtil;
import com.peach.common.validate.CommonValidator;
import com.peach.auth.dao.RoleDao;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.service.IRoleService;
import com.peach.auth.vo.RoleVO;
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

    @Resource
    private CommonValidator commonValidator;

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
        if (StringUtil.isBlank(id)){
            log.info("id is blank");
            return new RoleVO();
        }
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

    @Override
    public List<RoleVO> selectByUserCode(RoleQO roleQO) {
        return roleDao.selectByUser(roleQO);
    }
}
