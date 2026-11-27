
package com.peach.auth.service.impl;

import com.github.pagehelper.page.PageMethod;
import com.peach.code.CodeGenerator;

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
import java.util.Objects;
import java.util.UUID;
import com.peach.auth.dao.RoleWorkspaceDao;
import com.peach.auth.service.support.AdminScope;
import com.peach.common.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

/**
 * 角色服务实现类。
 *
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
    private final CodeGenerator codeGenerator;
    private final RoleWorkspaceDao workspaceDao;

    @Override
    public PageInfo<RoleVO> pageList(RoleQO roleQO) {
        roleQO.setTenantId(AdminScope.actor().getTenantId()); roleQO.setOrgId(AdminScope.actor().getOrgId());
        return PageMethod.startPage(roleQO.getPageNum(), roleQO.getPageSize())
                .doSelectPageInfo(() -> roleDao.selectByQO(roleQO));
    }

    @Override
    public List<RoleVO> list(RoleQO roleQO) {
        return roleDao.selectByUser(roleQO);
    }

    @Override
    public RoleVO selectById(String id) {
        var role = roleDao.selectById(id);
        if (role == null || Integer.valueOf(1).equals(role.getIsDelete()) || !Objects.equals(role.getTenantId(),AdminScope.actor().getTenantId()) || !Objects.equals(role.getOrgId(),AdminScope.actor().getOrgId())) throw new BusinessException("角色不存在或不可访问");
        return role;
    }

    @Override
    public void add(RoleDTO roleDTO) {
        RoleDO roleDO = new RoleDO();
        BeanUtils.copyProperties(roleDTO, roleDO);
        roleDO.setRoleId(UUID.randomUUID().toString().replace("-", ""));
        roleDO.setTenantId(AdminScope.actor().getTenantId());
        roleDO.setOrgId(AdminScope.actor().getOrgId());
        roleDO.setRoleCode(codeGenerator.next(roleDO.getTenantId(), "ROLE"));
        roleDO.setSkipUrl("");
        if (roleDO.getRoleScope() == null) roleDO.setRoleScope("ORG");
        if (roleDO.getRoleType() == null) roleDO.setRoleType("CUSTOM");
        roleDO.fillCreateTime(AdminScope.actor().getUserId());
        if (roleDO.getIsDelete() == null) {
            roleDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        }
        try { roleDao.insert(roleDO); } catch (DuplicateKeyException ex) { throw new BusinessException("当前机构下角色编码已存在"); }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void delById(String id) {
        var role = workspaceDao.lockRole(id,AdminScope.actor());
        if (role == null) throw new BusinessException("角色不存在或不可访问");
        if (workspaceDao.countReferences(role) > 0) throw new BusinessException("角色仍有用户、功能或资源授权，请先解除关联");
        roleDao.delById(id);
    }
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void update(RoleDTO input) {
        var current = workspaceDao.lockRole(input.getRoleId(),AdminScope.actor());
        if (current == null) throw new BusinessException("角色不存在或不可访问");
        if (input.getRoleCode() != null && !Objects.equals(input.getRoleCode(),current.getRoleCode())) throw new BusinessException("角色编码创建后不可修改");
        if (input.getSkipUrl() != null && !Objects.equals(input.getSkipUrl(),current.getSkipUrl())) throw new BusinessException("请通过默认菜单入口修改登录菜单");
        RoleDO change = new RoleDO(); BeanUtils.copyProperties(input,change);
        change.setRoleCode(current.getRoleCode()); change.setTenantId(current.getTenantId()); change.setOrgId(current.getOrgId()); change.setIsDelete(0); change.setSkipUrl(current.getSkipUrl());
        change.fillModifyTime(AdminScope.actor().getUserId()); roleDao.updateById(change);
    }

    @Override
    public List<RoleVO> selectByUserCode(RoleQO roleQO) {
        return roleDao.selectByUser(roleQO);
    }
}
