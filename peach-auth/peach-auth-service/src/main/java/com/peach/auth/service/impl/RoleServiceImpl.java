
package com.peach.auth.service.impl;

import com.github.pagehelper.page.PageMethod;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.dao.RoleDao;
import com.peach.auth.dto.RoleDTO;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.qo.RoleQO;
import com.peach.auth.service.IRoleService;
import com.peach.auth.vo.RoleVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:30
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {

        private final RoleDao roleDao;

    @Override
    public PageInfo<RoleVO> pageList(RoleQO roleQO) {
        return PageMethod.startPage(roleQO.getPageNum(), roleQO.getPageSize())
                .doSelectPageInfo(() -> roleDao.selectByQO(roleQO));
    }

    @Override
    public List<RoleVO> list(RoleQO roleQO) {
        return roleDao.selectByUser(roleQO);
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
    public void add(RoleDTO roleDTO) {
        RoleDO roleDO = new RoleDO();
        BeanUtils.copyProperties(roleDTO, roleDO);
        roleDO.fillCreateTime(null);
        if (roleDO.getIsDelete() == null) {
            roleDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        }
        roleDao.insert(roleDO);
    }

    @Override
    public void delById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return;
        }
        roleDao.delById(id);
    }

    @Override
    public void update(RoleDTO roleDTO) {
        RoleDO roleDO = new RoleDO();
        BeanUtils.copyProperties(roleDTO, roleDO);
        roleDO.fillModifyTime(null);
        roleDao.updateById(roleDO);
    }

    @Override
    public List<RoleVO> selectByUserCode(RoleQO roleQO) {
        return roleDao.selectByUser(roleQO);
    }
}
